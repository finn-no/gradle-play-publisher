package de.triplet.gradle.play;

class ValidatePublishApkTask extends PlayPublishApkTask {
    @Override
    def commit(edits, applicationId, editId) {
        edits.validate(applicationId, editId).execute()
    }
}
