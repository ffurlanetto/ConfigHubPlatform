/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user.dashboard;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Organization;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Path("/getDashboardElements")
public class GetDashboardElements
{
    private static final Logger log = LogManager.getLogger(GetDashboardElements.class);

    @GET
    @Produces("application/json")
    public Response get(@FormParam("startingId") Long startingId,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();

        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            Set<Repository> repositories = new HashSet<>();

            repositories.addAll(user.getRepositories());
            Set<Organization> organizations = user.getOrganizations();

            if (null != organizations)
                organizations.forEach(o -> repositories.addAll(o.getRepositories()));

            if (repositories.size() > 0)
            {
                JsonArray repositoriesMap = new JsonArray();
                repositories.forEach(r -> repositoriesMap.add(toJson(r)));
                json.add("repositories", repositoriesMap);
            }

            json.addProperty("success", true);
        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
        }
        finally
        {
            store.close();
        }


        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }

    private static JsonObject toJson(Repository repository)
    {
        JsonObject json = new JsonObject();

        json.addProperty("name", repository.getName());
        json.addProperty("id", repository.getId());
        json.addProperty("description", Utils.jsonString(repository.getDescription()));
        json.addProperty("account", repository.getAccountName());
        json.addProperty("isPrivate", repository.isPrivate());
        json.addProperty("isPersonal", repository.isPersonal());

        JsonObject depthMap = new JsonObject();
        repository.getDepth()
                  .getDepths()
                  .forEach(depth -> depthMap.addProperty(String.valueOf(depth.getPlacement()),
                                                 repository.getLabel(depth)));
        json.add("dls", depthMap);

        return json;
    }
}
