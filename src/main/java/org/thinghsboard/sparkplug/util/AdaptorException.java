/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thinghsboard.sparkplug.util;

/**
 * Created by nickAS21 on 10.01.23
 */
public class AdaptorException extends Exception {

    private static final long serialVersionUID = 1L;

    public AdaptorException() {
        super();
    }

    public AdaptorException(String cause) {
        super(cause);
    }

    public AdaptorException(Exception cause) {
        super(cause);
    }

    public AdaptorException(String message, Exception cause) {
        super(message, cause);
    }

}
