FROM mjenz/zerotier-dns

COPY ./zerotier-dns.yml /app/zerotier-dns.yml

WORKDIR /app

ENTRYPOINT ["./zerotier-dns"]
# api-key and network should be set corresponding to created ZeroTier network
CMD  ["server", "--api-key", "SX66tX8PEOurmjYGOdGLSoWqORNw2bUJ", "--network", "8286ac0e47f5088e"]
EXPOSE 53/udp
