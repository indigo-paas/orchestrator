#!/bin/bash

IFS=$'\n'; usable=($(oidc-add -l)); unset IFS
IFS=$'\n'; loaded=($(oidc-add -a)); unset IFS
missing=(`echo ${usable[@]} ${loaded[@]} | tr ' ' '\n' | sort | uniq -u `)
for account in "${missing[@]}"; do
    oidc-add $account
done