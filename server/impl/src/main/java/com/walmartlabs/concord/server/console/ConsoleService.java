package com.walmartlabs.concord.server.console;

import com.walmartlabs.concord.sdk.Secret;
import com.walmartlabs.concord.server.api.user.UserEntry;
import com.walmartlabs.concord.server.project.ProjectDao;
import com.walmartlabs.concord.server.project.RepositoryManager;
import com.walmartlabs.concord.server.security.UserPrincipal;
import com.walmartlabs.concord.server.security.ldap.LdapInfo;
import com.walmartlabs.concord.server.security.secret.SecretManager;
import com.walmartlabs.concord.server.user.UserManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.sonatype.siesta.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Named
@Path("/api/service/console")
public class ConsoleService implements Resource {

    private final ProjectDao projectDao;
    private final RepositoryManager repositoryManager;
    private final SecretManager secretManager;
    private final UserManager userManager;

    @Inject
    public ConsoleService(ProjectDao projectDao,
                          RepositoryManager repositoryManager,
                          SecretManager secretManager,
                          UserManager userManager) {

        this.projectDao = projectDao;
        this.repositoryManager = repositoryManager;
        this.secretManager = secretManager;
        this.userManager = userManager;
    }

    @GET
    @Path("/whoami")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public UserResponse whoami() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null) {
            throw new WebApplicationException("Can't determine current user: subject not found",
                    Status.INTERNAL_SERVER_ERROR);
        }

        UserPrincipal p = (UserPrincipal) subject.getPrincipal();
        if (p == null) {
            throw new WebApplicationException("Can't determine current user: entry not found",
                    Status.INTERNAL_SERVER_ERROR);
        }

        String displayName = null;

        LdapInfo ldapInfo = p.getLdapInfo();
        if (ldapInfo != null) {
            displayName = ldapInfo.getDisplayName();
        }

        if (displayName == null) {
            displayName = p.getUsername();
        }

        UserEntry user = userManager.get(p.getId())
                .orElseThrow(() -> new WebApplicationException("Unknown user: " + p.getId()));

        return new UserResponse(p.getRealm(), user.getName(), displayName, user.getTeams());
    }

    @POST
    @Path("/logout")
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null || !subject.isAuthenticated()) {
            return;
        }

        subject.logout();
    }

    @GET
    @Path("/project/{projectName}/exists")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public boolean isProjectExists(@PathParam("projectName") String projectName) {
        return projectDao.getId(projectName) != null;
    }

    @POST
    @Path("/repository/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    public boolean testRepository(RepositoryTestRequest req) {
        try {
            Secret secret = secretManager.getSecret(req.getSecret(), null);
            repositoryManager.testConnection(req.getUrl(), req.getBranch(), req.getCommitId(), req.getPath(), secret);
            return true;
        } catch (Exception e) {
            String msg;
            Throwable t = e;
            while (true) {
                msg = t.getMessage();
                t = t.getCause();
                if (t == null) {
                    break;
                }
            }

            if (msg == null) {
                msg = "Repository test error";
            }

            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                    .entity(msg)
                    .build());
        }
    }
}
