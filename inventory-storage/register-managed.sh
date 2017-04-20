#!/usr/bin/env bash

deployment_descriptor=${1:-DeploymentDescriptor-environment.json}
tenant_id=${2:-demo_tenant}
okapi_proxy_address=${3:-http://localhost:9130}
module_id=${4:-inventory-storage}

./okapi-registration/managed-deployment/register.sh \
  ${module_id} \
  ${okapi_proxy_address} \
  ${tenant_id} \
  ${deployment_descriptor}

