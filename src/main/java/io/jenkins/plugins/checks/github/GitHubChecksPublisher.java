package io.jenkins.plugins.checks.github;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GitHub;

import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;

import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksPublisher;

/**
 * A publisher which publishes GitHub check runs.
 */
public class GitHubChecksPublisher extends ChecksPublisher {
    private static final String GITHUB_URL = "https://api.github.com";

    private final GitHubChecksContext context;
    private final TaskListener listener;

    /**
     * {@inheritDoc}.
     *
     * @param context
     *         a context which contains SCM properties
     */
    public GitHubChecksPublisher(final GitHubChecksContext context, final TaskListener listener) {
        super();

        this.context = context;
        this.listener = listener;
    }

    /**
     * Publishes a GitHub check run.
     *
     * @param details
     *         the details of a check run
     */
    @Override
    public void publish(final ChecksDetails details) {
        try {
            GitHubAppCredentials credentials = context.getCredentials();
            GitHub gitHub = Connector.connect(StringUtils.defaultIfBlank(credentials.getApiUri(), GITHUB_URL),
                    credentials);
            GHCheckRunBuilder builder = createBuilder(gitHub, new GitHubChecksDetails(details));
            builder.create();
            listener.getLogger().println("GitHub checks have been published.");
        }
        catch (IllegalStateException | IOException e) {
            listener.getLogger().println("Failed Publishing GitHub checks: " + e);
        }
    }

    @VisibleForTesting
    GHCheckRunBuilder createBuilder(final GitHub gitHub, final GitHubChecksDetails details) throws IOException {
        GHCheckRunBuilder builder = gitHub.getRepository(context.getRepository())
                .createCheckRun(details.getName(), context.getHeadSha())
                .withStatus(details.getStatus())
                .withDetailsURL(details.getDetailsURL().orElse(context.getURL()))
                .withStartedAt(details.getStartedAt().orElse(Date.from(Instant.now())));

        if (details.getConclusion().isPresent()) {
            builder.withConclusion(details.getConclusion().get())
                    .withCompletedAt(details.getCompletedAt().orElse(Date.from(Instant.now())));
        }

        details.getOutput().ifPresent(builder::add);
        details.getActions().forEach(builder::add);

        return builder;
    }
}
