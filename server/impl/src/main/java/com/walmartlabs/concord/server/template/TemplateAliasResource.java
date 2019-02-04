package com.walmartlabs.concord.server.template;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.db.AbstractDao;
import com.walmartlabs.concord.server.security.Roles;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.apache.shiro.authz.UnauthorizedException;
import org.jooq.Configuration;
import org.sonatype.siesta.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Named
@Singleton
@Api(value = "TemplateAlias", authorizations = {@Authorization("api_key"), @Authorization("session_key"), @Authorization("ldap")})
@Path("/api/v1/template/alias")
public class TemplateAliasResource extends AbstractDao implements Resource {

    private final TemplateAliasDao aliasDao;

    @Inject
    public TemplateAliasResource(@Named("app") Configuration cfg, TemplateAliasDao aliasDao) {
        super(cfg);
        this.aliasDao = aliasDao;
    }

    @POST
    @ApiOperation("Create or update a template alias")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TemplateAliasResponse createOrUpdate(@ApiParam @Valid TemplateAliasEntry request) {
        assertAdmin();

        tx(tx -> {
            aliasDao.delete(request.getAlias());
            aliasDao.insert(request.getAlias(), request.getUrl());
        });

        return new TemplateAliasResponse();
    }

    @GET
    @ApiOperation("List current template aliases")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TemplateAliasEntry> list() {
        return aliasDao.list();
    }

    @DELETE
    @ApiOperation("Delete existing template alias")
    @Path("/{alias}")
    public TemplateAliasResponse delete(@PathParam("alias") @ConcordKey String alias) {

        assertAdmin();
        aliasDao.delete(alias);
        return new TemplateAliasResponse();
    }

    private static void assertAdmin() {
        if (!Roles.isAdmin()) {
            throw new UnauthorizedException("Only admins can do that");
        }
    }
}
