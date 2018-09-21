#!/bin/bash
#
# Copyright (C) 2015 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


# This script contains helper functions for ensuring that dependencies
# exist on a host system that are required to run Origin scripts.

# os::util::ensure::system_binary_exists ensures that the
# given binary exists on the system in the $PATH.
#
# Globals:
#  None
# Arguments:
#  - 1: binary to search for
# Returns:
#  None
function os::util::ensure::system_binary_exists() {
	local binary="$1"

	if ! os::util::find::system_binary "${binary}" >/dev/null 2>&1; then
		os::log::fatal "Required \`${binary}\` binary was not found in \$PATH."
	fi
}
readonly -f os::util::ensure::system_binary_exists

# os::util::ensure::built_binary_exists ensures that the
# given binary exists on the system in the local output
# directory for the current platform. If it doesn't, we
# will attempt to build it if we can determine the correct
# hack/build-go.sh target for the binary.
#
# This function will attempt to determine the correct
# hack/build-go.sh target for the binary, but may not
# be able to do so if the target doesn't live under
# cmd/ or tools/. In that case, one should be given.
#
# Globals:
#  - OS_ROOT
# Arguments:
#  - 1: binary to search for
#  - 2: optional build target for this binary
# Returns:
#  None
function os::util::ensure::built_binary_exists() {
	local binary="$1"
	local target="${2:-}"

	if ! os::util::find::built_binary "${binary}" >/dev/null 2>&1; then
		if [[ -z "${target}" ]]; then
			if [[ -d "${OS_ROOT}/cmd/${binary}" ]]; then
				target="cmd/${binary}"
			elif [[ -d "${OS_ROOT}/tools/${binary}" ]]; then
				target="tools/${binary}"
			elif [[ -d "${OS_ROOT}/tools/rebasehelpers/${binary}" ]]; then
				target="tools/rebasehelpers/${binary}"
			fi
		fi

		if [[ -n "${target}" ]]; then
			os::log::warning "No compiled \`${binary}\` binary was found. Attempting to build one using:
  $ hack/build-go.sh ${target}"
			"${OS_ROOT}/hack/build-go.sh" "${target}"
		else
			os::log::fatal "No compiled \`${binary}\` binary was found and no build target could be determined.
Provide the binary and try running $0 again."
		fi
	fi
}
readonly -f os::util::ensure::built_binary_exists

# os::util::ensure::gopath_binary_exists ensures that the
# given binary exists on the system in $GOPATH.
#
# Globals:
#  - GOPATH
# Arguments:
#  - 1: binary to search for
# Returns:
#  None
function os::util::ensure::gopath_binary_exists() {
	local binary="$1"

	if ! os::util::find::gopath_binary "${binary}" >/dev/null 2>&1; then
		os::log::fatal "Required \`${binary}\` binary was not found in \$GOPATH."
	fi
}
readonly -f os::util::ensure::gopath_binary_exists

# os::util::ensure::iptables_privileges_exist tests if the
# testing machine has iptables available and in PATH. Also
# tests that the user can list iptables rules, trying with
# `sudo` if it fails without.
#
# Globals:
#  None
# Arguments:
#  None
# Returns:
#  None
function os::util::ensure::iptables_privileges_exist() {
	os::util::ensure::system_binary_exists 'iptables'

	if ! iptables --list >/dev/null 2>&1 && ! sudo iptables --list >/dev/null 2>&1; then
		os::log::fatal "You do not have \`iptables\` or \`sudo\` privileges. Kubernetes services will not work
without \`iptables\` access. See https://github.com/kubernetes/kubernetes/issues/1859."
	fi
}
readonly -f os::util::ensure::iptables_privileges_exist