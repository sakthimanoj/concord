package com.walmartlabs.concord.runtime.v2.runner;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.dependencymanager.DependencyEntity;
import com.walmartlabs.concord.runtime.common.cfg.RunnerConfiguration;
import com.walmartlabs.concord.runtime.v2.sdk.DependencyManager;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultDependencyManager implements DependencyManager {

    private final com.walmartlabs.concord.dependencymanager.DependencyManager delegate;

    @Inject
    public DefaultDependencyManager(RunnerConfiguration cfg) {
        try {
            this.delegate = new com.walmartlabs.concord.dependencymanager.DependencyManager(getCacheDir(cfg));
        } catch (IOException e) {
            throw new RuntimeException("Error while initializing DependencyManager: " + e.getMessage());
        }
    }

    @Override
    public Path resolve(URI uri) throws IOException {
        DependencyEntity entity = delegate.resolveSingle(uri);
        return entity.getPath();
    }

    private static Path getCacheDir(RunnerConfiguration cfg) {
        try {
            String s = cfg.dependencyManager().cacheDir();
            if (s == null) {
                return IOUtils.createTempDir("dependencyCache");
            }

            Path p = Paths.get(s);
            if (!Files.exists(p) || !Files.isDirectory(p)) {
                throw new RuntimeException("The dependency cache directory doesn't exist or not a directory: " + p);
            }

            return p;
        } catch (IOException e) {
            throw new RuntimeException("Error while creating the dependency cache directory", e);
        }
    }
}
