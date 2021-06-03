#!/bin/sh

HYDRA_VERSION="v1.10.2"

mkdir -p hydra-generated

docker run --user 1000 --rm -v "${PWD}/hydra-generated:/local" openapitools/openapi-generator-cli generate \
    -i "https://raw.githubusercontent.com/ory/hydra/${HYDRA_VERSION}/spec/api.json" \
    -g kotlin \
    --additional-properties modelPackage=sh.ory.hydra.model,serializationLibrary=jackson,swaggerAnnotations=false,hideGenerationTimestamp=true \
    -o /local
