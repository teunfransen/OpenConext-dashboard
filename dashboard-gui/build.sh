#!/bin/bash
rm -Rf build/*
rm -Rf target/*
yarn install --force
yarn test
echo "BUILDING CSS"
yarn build-css
echo "BUILDING JS"
yarn build
