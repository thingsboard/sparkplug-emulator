#!/bin/bash
#
# Copyright © 2016-2025 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

cd "$SCRIPTPATH" || exit 1
mvn clean package -DskipTests || exit 1

cd "${SCRIPTPATH}/target" || exit 1

docker buildx build --no-cache -f ./Dockerfile --tag thingsboard/tb-sparkplug-emulator:latest .

read -r -p "Push image to Docker Hub? [Y/n]: " PUSH_CHOICE

if [[ "$PUSH_CHOICE" =~ ^[Yy]$ || -z "$PUSH_CHOICE" ]]; then
    docker push thingsboard/tb-sparkplug-emulator:latest
else
    echo "Skipping push."
fi

# Example of running a container locally for testing:
# docker run --rm -it thingsboard/tb-sparkplug-emulator:latest

