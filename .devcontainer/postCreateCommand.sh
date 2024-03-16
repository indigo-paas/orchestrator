echo alias export-oidc-env=\'eval \$\(more /oidc-agent-config/oidc-agent.env\)\' >> /home/node/.bashrc
echo alias orchent=\'export-oidc-env \&\& docker run --rm -e ORCHENT_URL=\$ORCHENT_URL -e OIDC_SOCK=\$OIDC_SOCK -e ORCHENT_AGENT_ACCOUNT=\$ORCHENT_AGENT_ACCOUNT indigodatacloud/orchent\' >> /home/node/.bashrc
echo alias oidc-add=\'docker exec -it orchestrator-devc-oidc-agent oidc-add\' >> /home/node/.bashrc
echo alias oidc-gen=\'docker exec -it orchestrator-devc-oidc-agent oidc-gen\' >> /home/node/.bashrc
echo alias oidc-token=\'docker exec orchestrator-devc-oidc-agent oidc-token\' >> /home/node/.bashrc
