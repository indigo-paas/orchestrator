echo alias oidc-add=\'docker exec -it orchestrator-devc-oidc-agent oidc-add\' >> /home/node/.bashrc
echo alias oidc-gen=\'docker exec -it orchestrator-devc-oidc-agent oidc-gen\' >> /home/node/.bashrc
echo alias oidc-token=\'docker exec orchestrator-devc-oidc-agent oidc-token\' >> /home/node/.bashrc
# echo alias export-oidc-env=\'eval \$\(more /oidc-agent-config/oidc-agent.env \| sed \'s\/tmp\/oidc-agent-config\/\'\)\' >> /home/node/.bashrc
echo alias export-oidc-token=\'export ORCHENT_TOKEN=\$\(oidc-token \$ORCHENT_AGENT_ACCOUNT \)\' >> /home/node/.bashrc
# echo alias orchent=\'export-oidc-env \&\& orchent\' >> /home/node/.bashrc
echo alias orchent=\'export-oidc-token \&\& orchent\' >> /home/node/.bashrc