FROM mcr.microsoft.com/devcontainers/typescript-node:18

# Install orchent
RUN wget https://github.com/indigo-dc/orchent/releases/download/v1.2.9/orchent_1.2.9_amd64.deb && \
    dpkg -i orchent_1.2.9_amd64.deb && \
    rm -rf orchent_1.2.9_amd64.deb
