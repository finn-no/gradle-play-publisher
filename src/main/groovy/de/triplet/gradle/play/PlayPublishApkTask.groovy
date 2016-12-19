package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    static def MIME_TYPE_APK = "application/vnd.android.package-archive"
    static def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    static def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    File inputFolder

    @TaskAction
    publishApks() {
        super.publish()

        try {
            List<Integer> versionCodes = new ArrayList<Integer>()

            variant.outputs
                    .findAll { variantOutput -> variantOutput instanceof ApkVariantOutput }
                    .each { variantOutput -> versionCodes.add(publishApk(new FileContent(MIME_TYPE_APK, variantOutput.outputFile)).getVersionCode()) }

            Track track = new Track().setVersionCodes(versionCodes)
            if (extension.track?.equals("rollout")) {
                def oldTrack = edits.tracks()
                        .list(variant.applicationId, editId)
                        .execute()
                        .getTracks()
                        .find() { oldTrack -> oldTrack.getTrack().equals("rollout") }

                if (oldTrack != null) {
                    track.setUserFraction(oldTrack.getUserFraction())
                } else {
                    track.setUserFraction(extension.userFraction as Double)
                }
            }
            edits.tracks()
                    .update(variant.applicationId, editId, extension.track, track)
                    .execute()

            commit(edits, variant.applicationId, editId)
        } catch (GoogleJsonResponseException e) {

            List<GoogleJsonError.ErrorInfo> errors = ((GoogleJsonResponseException) e).getDetails().getErrors();
            if (!(errors.size() == 1 && "apkUpgradeVersionConflict".equals(errors.get(0).getReason()) && errors.get(0)
                    .getMessage()
                    .contains("APK specifies a version code that has already been used."))) {
                throw e;
            }
        }
    }

    def commit(edits, applicationId, editId) {
        edits.commit(applicationId, editId).execute()
    }

    def Apk publishApk(apkFile) {

        Apk apk = edits.apks()
                .upload(variant.applicationId, editId, apkFile)
                .execute()

        if (extension.untrackOld && !"alpha".equals(extension.track)) {
            def untrackChannels = "beta".equals(extension.track) ? ["alpha"] : ["alpha", "beta"]
            untrackChannels.each { channel ->
                Track track = edits.tracks().get(variant.applicationId, editId, channel).execute()
                track.setVersionCodes(track.getVersionCodes().findAll {
                    it > apk.getVersionCode()
                });

                edits.tracks().update(variant.applicationId, editId, channel, track).execute()
            }
        }

        if (inputFolder.exists()) {

            // Matches if locale have the correct naming e.g. en-US for play store
            inputFolder.eachDirMatch(matcher) { dir ->
                File whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT + "-" + extension.track)

                if (!whatsNewFile.exists()) {
                    whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT)
                }

                if (whatsNewFile.exists()) {

                    def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT, extension.errorOnSizeLimit)
                    def locale = dir.name

                    ApkListing newApkListing = new ApkListing().setRecentChanges(whatsNewText)
                    edits.apklistings()
                            .update(variant.applicationId, editId, apk.getVersionCode(), locale, newApkListing)
                            .execute()
                }
            }
        }

        return apk
    }

}
