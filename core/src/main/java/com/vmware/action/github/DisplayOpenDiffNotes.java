package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestCommitVersion;
import com.vmware.gitlab.domain.MergeRequestDiscussion;
import com.vmware.gitlab.domain.MergeRequestNote;
import com.vmware.gitlab.domain.User;
import com.vmware.util.logging.Padder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@ActionDescription("Displays open diff and general comments for review.")
public class DisplayOpenDiffNotes extends BaseCommitWithMergeRequestAction {
    private final Map<String, String[]> diffFiles = new HashMap<>();

    public DisplayOpenDiffNotes(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        mergeRequest = gitlab.getMergeRequest(mergeRequest.projectId, mergeRequest.iid);
        log.debug("Head sha for merge request {}", mergeRequest.diffRefs.headSha);
        Set<MergeRequestDiscussion> discussions = gitlab.getOpenMergeRequestDiscussions(mergeRequest.projectId, mergeRequest.iid);

        MergeRequestCommitVersion[] commitVersions = null;
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd hh:mm:ss");
        Map<User, List<MergeRequestNote>> diffNotesPerAuthor = new LinkedHashMap<>();
        for (MergeRequestDiscussion discussion : discussions) {
            MergeRequestNote firstNote = discussion.notes[0];
            if (firstNote.author.username.equals(mergeRequest.author.username)) {
                continue;
            }
            diffNotesPerAuthor.computeIfAbsent(firstNote.author, key -> new ArrayList<>()).add(firstNote);
        }

        for (User author : diffNotesPerAuthor.keySet()) {
            Padder authorPadder = new Padder(author.name);
            authorPadder.infoTitle();
            List<MergeRequestNote> notes = diffNotesPerAuthor.get(author);
            notes.sort(Comparator.comparing(MergeRequestNote::createdDate));
            for (MergeRequestNote note : notes) {
                if (commitVersions == null) {
                    commitVersions = gitlab.getOpenMergeRequestCommitVersions(mergeRequest.projectId, mergeRequest.iid);
                }
                Padder notePadder = new Padder(60, formatter.format(note.createdAt));
                notePadder.infoTitle();
                printDiffLines(note, commitVersions);
                log.info(note.body);
                notePadder.infoTitle();
            }
            authorPadder.infoTitle();
        }
    }

    private void printDiffLines(MergeRequestNote note, MergeRequestCommitVersion[] commitVersions) {
        MergeRequestNote.Position position = note.position;
        if (!"text".equalsIgnoreCase(position.positionType)) {
            log.info(position.newPath + " - " + position.positionType);
            log.debug("Not printing diff lines for diff type {}", position.positionType);
            return;
        }

        log.debug("Base {} Start {} Head {}", position.baseSha, position.startSha, position.headSha);
        MergeRequestNote.LineRange lineRange = position.lineRange;
        if (lineRange.start.oldLine != null) {
            log.info("{}", position.oldPath);
            int endLine = lineRange.end.oldLine != null ? lineRange.end.oldLine : lineRange.start.oldLine;
            String[] diffFileContents = diffFiles.computeIfAbsent(position.startSha + ":" + position.oldPath,
                    key -> git.show(key).split(System.lineSeparator()));
            printDiffFileLines(diffFileContents, lineRange.start.oldLine, endLine, "-");
        }
        if (lineRange.start.newLine != null) {
            MergeRequestCommitVersion matchingVersion = findVersionForDate(commitVersions, note.createdAt);
            if (!matchingVersion.headCommitSha.equals(position.headSha)) {
                log.debug("Wrong head sha {}, should be {}", position.headSha, matchingVersion.headCommitSha);
            }
            if (lineRange.start.oldLine == null || !position.newPath.equals(position.oldPath)) {
                log.info("{}", position.newPath);
            }
            String[] diffFileContents = diffFiles.computeIfAbsent(matchingVersion.headCommitSha + ":" + position.newPath,
                    key -> git.show(key).split(System.lineSeparator()));
            int lineStart = lineRange.start.newLine;
            int lineEnd = lineRange.end.newLine != null ? lineRange.end.newLine : lineStart;
            printDiffFileLines(diffFileContents,lineStart, lineEnd, "+");
        }
    }

    private void printDiffFileLines(String[] diffFileContents, int start, int end, String prefix) {
        if (start == end) {
            log.info("{} {} {}", start, prefix, diffFileContents[start - 1]);
        } else {
            IntStream.rangeClosed(start, end).forEach(index -> {
                log.info("{} {} {}", index, prefix, diffFileContents[index - 1]);
            });
        }
        log.info("");
    }

    private MergeRequestCommitVersion findVersionForDate(MergeRequestCommitVersion[] versions, Date date) {
        for (MergeRequestCommitVersion version : versions) {
            if (!version.createdAt.after(date)) {
                return version;
            }
        }
        log.info("No commit version was before {}", date);
        return versions[0];
    }
}
