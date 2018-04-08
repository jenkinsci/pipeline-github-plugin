Table of Contents
=================
  * [Pipeline: GitHub](#pipeline-github)
  * [License](#license)
  * [Prerequisites](#prerequisites)
  * [Credentials](#credentials)
  * [Triggers](#triggers)
    * [issueCommentTrigger](#issuecommenttrigger)
  * [Global Variables](#global-variables)
    * [pullRequest](#pullrequest)
  * [Auxiliary Classes](#auxiliary-classes)
    * [CommitStatus](#commitstatus)
    * [Commit](#commit)
    * [CommitFile](#commitfile)
    * [IssueComment](#issuecommet)
    * [ReviewComment](#reviewcomment)
  * [Examples](#examples)


# Pipeline: GitHub
The entry points for this plugin’s functionality are additional global variables, available to pipeline scripts when the plugin is enabled and the prerequisites are met. 

# License
MIT

# Prerequisites

- Jenkins running Java 8 or higher.

- Projects/jobs must be automatically created by the GitHub Organization folder/project type. 

See: [GitHub Branch Source Plugin](https://go.cloudbees.com/docs/cloudbees-documentation/cje-user-guide/index.html#github-branch-source)

# Credentials

Currently all operations against GitHub will be performed using the builds `GitHubSCMSource` credentials. These will typically be the `Scan Credentials` you configured in your GitHub Organization. 

However you can override this in a pipeline script by calling `setCredentials(String userName, String password)` before any properties or methods are accessed/invoked on the `pullRequest` global variable.

```groovy
pullRequest.setCredentials('John.Smith', 'qwerty4321')
```

If you plan to use this plugin to add/modify/remove comments, labels, commit statuses etc. Please ensure that the required permissions are assigned to the token supplied in the credentials (`Scan Credentials` or `Manually` supplied).

# Triggers

This plugin adds the following pipeline triggers

## issueCommentTrigger

### Requirements

- This trigger only works on Pull Requests, created by the GitHub Branch Source Plugin.
- Currently this trigger will only allow collaborators of the repository in question to trigger builds.

### Limitations

The Pull Request's job/build must have run at least once for the trigger to be registered. If an initial run never takes place then the trigger won't be registered and cannot pickup on any comments made. 

This should not be an issue in practice, because a requirement of using this plugin is that your jobs are setup automatically by the GitHub Branch Source Plugin, which will trigger an initial build when it is notified of a new Pull Request. 

### Considerations

This trigger would be of limited usefulness for people wishing to build public GitHub/Jenkins bots, using pipeline scripts. As there is no way to ensure that a Pull Request's `Jenkinsfile` contains any triggers. Not to mention you would not want to trust just any `Jenkinsfile` from a random Pull Request/non-collaborator.

This trigger is intended to be used inside enterprise organizations: 
1. Where all branches and forks just contain a token `Jenkinsfile` that delegates to the real pipeline script, using [shared libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/).
2. Trust all their Pull Request authors.

### Parameters

- `commentPattern` (__Required__) - A Java style regular expression

### Usage

#### Scripted Pipeline:
```groovy
properties([
    pipelineTriggers([
        issueCommentTrigger('.*test this please.*')
    ])
])
```

#### Declarative Pipeline:
```groovy
pipeline {
    triggers {
        issueCommentTrigger('.*test this please.*')
    }
}
```

#### Detecting whether a build was started by the trigger in a script:
```groovy
def triggerCause = currentBuild.rawBuild.getCause(
    org.jenkinsci.plugins.pipeline.github.trigger.IssueCommentCause
)

if (triggerCause) {
    echo("Build was started by ${triggerCause.userLogin}, who wrote: " +
         "\"${triggerCause.comment}\", which matches the " +
         "\"${triggerCause.triggerPattern}\" trigger pattern.")
} else {
    echo('Build was not started by a trigger')
}
```

# Global Variables

## `repository`
Coming soon!

## `pullRequest`

### Usage

Before you can use the `pullRequest` global variable you must ensure you are actually in a Pull Request build job. The best way to do this is to check for the existence of the `CHANGE_ID` environment variable.

#### Scripted Pipeline:
```groovy
node {
    stage('Build') {
        try {
            echo 'Hello World'
        } catch (err) {
            // CHANGE_ID is set only for pull requests, so it is safe to access the pullRequest global variable
            if (env.CHANGE_ID) {
                pullRequest.addLabel('Build Failed')
            }
            throw err
        }
    }
}
```

#### Declarative Pipeline:
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Hello World'
            }
        }
    }
    post {
        failure {
            script {
                // CHANGE_ID is set only for pull requests, so it is safe to access the pullRequest global variable
                if (env.CHANGE_ID) {
                    pullRequest.addLabel('Build Failed')
                }
            }
        }
    }
}
```

### Properties

Name | Type | Setter   | Description
-----|------|----------|------------
id | `Integer` | false | 
state | `String` | **true** | Valid values `open` or `closed`
number | `Integer` | false
url | `String` | false
patchUrl | `String` | false
diffUrl | `String` | false
issueUrl | `String` | false
title | `String` | **true**
body | `String` | **true**
locked | `Boolean` | **true** | Accepts `true`, `false` or `'true'`, `'false'`
milestone | `Integer` | **true**
head | `String` | false | Revision (SHA) of the head commit of this pull request
headRef | `String` | false | Name of the branch this pull request is created for
base | `String` | **true** | Name of the base branch in the current repository this pull request targets
files | `Iterable<CommitFile>` | false
assignees | `List<String>` | **true** | Accepts a `List<String>`
commits | `Iterable<Commit>` | false
comments | `Iterable<IssueComment>` | false
reviewComments | `Iterable<ReviewComment>` | false
labels | `Iterable<String>` | **true** | Accepts a `List<String>`
statuses | `List<CommitStatus>` | false 
requestedReviewers | `Iterable<String>` | false
updatedAt | `Date` | false
createdAt | `Date` | false
createdBy | `String` | false
closedAt | `Date` | false
closedBy | `String` | false
mergedAt | `Date` | false
mergedBy | `String` | false
commitCount | `Integer` | false
commentCount | `Integer` | false
additions | `Integer` | false
deletions | `Integer` | false
changedFiles | `Integer` | false
merged | `Boolean` | false
mergeable | `Boolean` | false
mergeCommitSha | `String` | false
maintainerCanModify | `Boolean` | **true** | Accepts `true`, `false` or `'true'`, `'false'`


### Methods

#### Merge

> String merge(__[String commitTitle, String commitMessage, String sha, String mergeMethod]__)

Returns the merge's SHA/commit id.

#### Commit Status

> CommitStatus createStatus(String status __[, String context, String description, String targetUrl]__)

#### Labels
> void addLabels(List<String> labels)

> void removeLabel(String label)

#### Assignees
> void addAssignees(List<String> assignees)

> void removeAssignees(List<String> assignees)

#### Review Comments
> ReviewComment reviewComment(String commitId, String path, int position, String body)

> ReviewComment editReviewComment(long commentId, String body)

> ReviewComment replyToReviewComment(long commentId, String body)

> void deleteReviewComment(long commentId)

#### Pull Request Comments (Issue Comments)
> IssueComment comment(String body)

> IssueComment editComment(long commentId, String body)

> void deleteComment(long commentId)

### Requested Reviewers
> void createReviewRequests(List<String> reviewers)

> void deleteReviewRequests(List<String> reviewers)

#### Misc
> void setCredentials(String userName, String password)

# Auxiliary Classes

## CommitStatus
### Properties
Name | Type | Setter | Description
-----|------|----------|------------
id | `String` | false | 
url | `String` | false
status | `String` | false | One of `pending`, `success`, `failure` or `error`
context | `String` | false
description | `String` | false
targetUrl | `String` | false
createdAt | `Date` | false
updatedAt | `Date` | false
creator | `String` | false

### Methods
None.

## Commit
### Properties
Name | Type | Setter | Description
-----|------|----------|------------
sha | `String` | false
url | `String` | false
author | `String` | false
committer | `String` | false
parents | `List<String>` | false | List of parent commit SHA's
message | `String` | false |
commentCount | `Integer` | false
comments | `Iterable<ReviewComment>` | false 
additions | `Integer` | false
deletions | `Integer` | false
totalChanges | `Integer` | false
files | `List<CommitFile>` | false | List of files added, removed and or modified in this commit
statuses | `List<CommitStatus>` | false | List of statuses associated with this commit

### Methods
#### Commit Status
> CommitStatus createStatus(String status __[, String context, String description, String targetUrl]__)

#### Review Comment
> ReviewComment comment(String body __[, String path, Integer position]__)

## CommitFile
### Properties
Name | Type | Setter | Description
-----|------|----------|------------
sha | `String` | false
filename | `String` | false
status | `String` | false | One of 
patch | `String` | false
additions | `Integer` | false
deletions | `Integer` | false
changes | `Integer` | false
rawUrl | `String` | false
blobUrl | `String` | false

### Methods
None.

## IssueComment
### Properties
Name | Type | Setter | Description
-----|------|----------|------------
id | `Integer` | false
url | `String` | false
user | `String` | false
body | `String` | **true**
createdAt | `Date` | false
updatedAt | `Date` | false

### Methods
> void delete()

## ReviewComment
### Properties
Name | Type | Setter | Description
-----|------|----------|------------
id | `Integer` | false
url | `String` | false
user | `String` | false
createdAt | `Date` | false
updatedAt | `Date` | false
commitId | `String` | false
originalCommitId | `Integer` | false
body | `String` | **true**
path | `String` | false
line | `Integer` | false
position | `Integer` | false
originalPosition | `Integer` | false
diffHunk | `String` | false

### Methods
> void delete()


# Examples

## Pull Requests

### Updating a Pull Request's title and body
```groovy
pullRequest.title = 'Updated title'
pullRequest.body = pullRequest.body + '\nEdited by Pipeline'
```

### Closing a Pull Request
```groovy
pullRequest.status = 'closed'
```

### Creating a Commit Status against the head of the Pull Request
```groovy
pullRequest.createStatus(status: 'success',
                         context: 'continuous-integration/jenkins/pr-merge/tests',
                         description: 'All tests are passing',
                         targetUrl: "${JOB_URL}/testResults")
```

### Locking and unlocking a Pull Request's conversation
```groovy
if (pullRequest.locked) {
    pullRequest.locked = false
}
```

### Merging a Pull Request
```groovy
if (pullRequest.mergeable) {
    pullRequest.merge('merge commit message here')
}
// or
if (pullRequest.mergeable) {
    pullRequest.merge(commitTile: 'Make it so..', commitMessage: 'TO BOLDLY GO WHERE NO MAN HAS GONE BEFORE...', mergeMethod: 'squash')
}
```

### Adding a label
```groovy
pullRequest.addLabel('Build Passing')
```

### Removing a label
```groovy
pullRequest.removeLabel('Build Passing')
```

### Replacing all labels
```groovy
pullRequest.labels = ['Bug', 'Feature']
```

### Adding an assignee
```groovy
pullRequest.addAssignee('Spock')
```

### Removing an assignee
```groovy
pullRequest.removeAssignee('McCoy')
```

### Replacing all assignees
```groovy
pullRequest.assignees = ['Data', 'Scotty']
```

### Listing all added/modified/removed files
```groovy
for (commitFile in pullRequest.files) {
    echo "SHA: ${commitFile.sha} File Name: ${commitFile.filename} Status: ${commitFile.status}"
}
```

### Adding a comment
```groovy
def comment = pullRequest.comment('This PR is highly illogical..')
```

### Editing a comment
```groovy
pullRequest.editComment(comment.id, 'Live long and prosper.')
// or
comment.body = 'Live long and prosper.'
```

### Deleting a comment
```groovy
pullRequest.deleteComment(commentId)
// or
comment.delete()
```

### Adding a review comment
```groovy
def commitId = 'SHA of the commit containing the change/file you wish to review';
def path = 'src/main/java/Main.java'
def lineNumber = 5
def body = 'The review comment'
def comment = pullRequest.reviewComment(commitId, path, lineNumber, body)
```

### Editing a review comment
```groovy
pullRequest.editReviewComment(comment.id, 'Live long and prosper.')
// or
comment.body = 'Live long and prosper.'
```

### Deleting a review comment
```groovy
pullRequest.deleteReviewComment(comment.id)
// or
comment.delete()
```

### Replying to a review comment
```groovy
pullRequest.replyToReviewComment(comment.id, 'Khaaannnn!')
// or
comment.createReply('Khaaannnn!')
```

### Listing a Pull Request's commits
```groovy
for (commit in pullRequest.commits) {
   echo "SHA: ${commit.sha}, Committer: ${commit.committer}, Commit Message: ${commit.message}"
}
```

### Listing a Pull Request's comments
```groovy
for (comment in pullRequest.comments) {
  echo "Author: ${comment.user}, Comment: ${comment.body}"
}
```

### Listing a Pull Request's review comments
```groovy
for (reviewComment in pullRequest.reviewComments) {
  echo "File: ${reviewComment.path}, Line: ${reviewComment.line}, Author: ${reviewComment.user}, Comment: ${reviewComment.body}"
} 
```

### Listing a commit's statuses
```groovy
for (commit in pullRequest.commits) {
  for (status  in commit.statuses) {
     echo "Commit: ${commit.sha}, Status: ${status.status}, Context: ${status.context}, URL: ${status.targetUrl}"
  }
}
```

### Creating a Commit Status against arbitrary commits
```groovy
for (commit in pullRequest.commits) {
  commit.createStatus(status: 'pending')
}
```

### Listing a Pull Request's current statuses
```groovy
for (status in pullRequest.statuses) {
  echo "Commit: ${pullRequest.head}, Status: ${status.status}, Context: ${status.context}, URL: ${status.targetUrl}"
}
```

### Listing a Pull Request's requested reviewers
```groovy
for (requestedReviewer in pullRequest.requestedReviewers) {
  echo "${requestedReviewer} was requested to review this Pull Request"
}
```

### Requesting reviewers
```groovy
pullRequest.createReviewRequests('Spock', 'McCoy')
```

### Deleting requested reviewers
```groovy
pullRequest.deleteReviewRequests('McCoy')
```
