package act.aaa.util;

/*-
 * #%L
 * ACT AAA Plugin
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
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
 * #L%
 */

import act.Act;
import act.aaa.AAAConfig;
import act.aaa.AAAPlugin;
import act.aaa.model.UserBase;
import act.app.App;
import act.db.Dao;
import org.osgl.$;
import org.osgl.aaa.*;
import org.osgl.inject.ValueLoader;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Load user from logged in principal
 */
public class LoginUserFinder extends ValueLoader.Base {

    public static final String KEY_USER_KEY = "key";

    private Dao dao;
    private String querySpec;

    @Override
    public Object get() {
        AAAContext aaaContext = AAA.context();
        if (null != aaaContext) {
            Principal principal = aaaContext.getCurrentPrincipal();
            if (null != principal) {
                if (principal instanceof UserBase) {
                    return principal;
                }
                String querySpec = this.querySpec;
                AAAPlugin plugin = Act.getInstance(AAAPlugin.class);
                if ("id".equals(querySpec)) {
                    String s = principal.getProperty("id");
                    E.unexpectedIf(S.isBlank(s), "Cannot determine id of principal");
                    Object id = $.convert(s).to(dao.idType());
                    String cacheKey = S.string(id);
                    Object cached = plugin.cachedUser(cacheKey);
                    if (null == cached) {
                        cached = dao.findById(id);
                        if (null != cached) {
                            plugin.cacheUser(cacheKey, cached);
                        }
                    }
                    return cached;
                }
                String name = principal.getName();
                int pos = name.indexOf(':');
                if (pos > 0) {
                    querySpec = name.substring(0, pos);
                    name = name.substring(pos + 1);
                }
                String cacheKey = S.concat(querySpec, "::", name);
                Object cached = plugin.cachedUser(cacheKey);
                if (null == cached) {
                    cached = dao.findOneBy(querySpec, name);
                    if (null != cached) {
                        plugin.cacheUser(cacheKey, cached);
                    }
                }
                return cached;
            }
        }
        return null;
    }

    @Override
    protected void initialized() {
        App app = App.instance();

        Class rawType = spec.rawType();
        dao = app.dbServiceManager().dao(rawType);

        querySpec = S.string(options.get(KEY_USER_KEY));
        if (S.blank(querySpec)) {
            querySpec = AAAConfig.user.key.get();
        }
    }
}
