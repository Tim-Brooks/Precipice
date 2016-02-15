/*
 * Copyright 2016 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.samples.http;

import net.uncontended.precipice.Failable;

public enum HTTPStatus implements Failable {
    STATUS_200(false),
    NON_200(false),
    ERROR(true),
    TIMEOUT(true);

    private final boolean isFailed;

    HTTPStatus(boolean isFailed) {
        this.isFailed = isFailed;
    }

    @Override
    public boolean isFailure() {
        return isFailed;
    }

    @Override
    public boolean isSuccess() {
        return !isFailed;
    }
}
