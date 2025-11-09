#!/bin/sh

HYDRA_VERSION="v25.4.0"
USER=1000

mkdir -p hydra-generated

docker run --rm -v "${PWD}/hydra-generated:/local" openapitools/openapi-generator-cli generate \
    -i "https://raw.githubusercontent.com/ory/hydra/${HYDRA_VERSION}/spec/api.json" \
    -g kotlin \
    --additional-properties modelPackage=sh.ory.hydra.model,serializationLibrary=jackson \
    -o /local

sudo chown -R $USER:$USER hydra-generated
