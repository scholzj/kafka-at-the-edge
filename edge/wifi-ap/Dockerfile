FROM registry.fedoraproject.org/fedora-minimal:37-aarch64

RUN microdnf install -y --setopt=install_weak_deps=0 hostapd dnsmasq iproute

COPY run.sh /run.sh

CMD ["/run.sh"]