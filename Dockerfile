FROM ubuntu:18.04

LABEL description="HALO / webOS Yocto build environment"

# Enables us to overwrite the user and group ID for the yoctouser. See below
ARG userid=1003
ARG groupid=1003

USER root

# Install dependencies in one command to avoid potential use of previous cache
# like explained here: https://stackoverflow.com/a/37727984
RUN apt-get update && \
    apt-get install -y \
        bc \
        build-essential \
        cmake \
        chrpath \
        coreutils \
        cpio \
        curl \
        cvs \
        debianutils \
        diffstat \
        g++-multilib \
        gawk \
        gcc-multilib \
        git-core \
        graphviz \
        help2man \
        iptables \
        iputils-ping \
        libegl1-mesa \
        libfdt1 \
        libsdl1.2-dev \
        libxml2-utils \
        locales \
        m4 \
        openssh-server \
        python \
        python-pysqlite2 \
        python3 \
        python3-git \
        python3-jinja2 \
        python3-pexpect \
        python3-pip \
        qemu-user \
        repo \
        rsync \
        screen \
        socat \
        subversion \
        sudo \
        sysstat \
        texinfo \
        tmux \
        unzip \
        wget \
        xz-utils

RUN apt-get clean

# For Yocto bitbake -c testimage XML reporting
RUN pip3 install unittest-xml-reporting

# For git-lfs
# The downloaded script is needed since git-lfs is not available per default for Ubuntu
RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash && sudo apt-get install -y git-lfs

# Remove all apt lists to avoid build caching
RUN rm -rf /var/lib/apt/lists/*

# en_US.utf8 is required by Yocto sanity check
RUN /usr/sbin/locale-gen en_US.UTF-8
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
RUN echo 'export LC_ALL="en_US.UTF-8"' >> /etc/profile
ENV LANG en_US.utf8

RUN useradd -U -m yoctouser

# Make sure the user/groupID matches the UID/GID given to Docker. This is so that mounted
# dirs will get the correct permissions
RUN usermod --uid $userid yoctouser
RUN groupmod --gid $groupid yoctouser
RUN echo 'yoctouser:yoctouser' | chpasswd
RUN echo 'yoctouser ALL=(ALL) NOPASSWD:SETENV: ALL' > /etc/sudoers.d/yoctouser

# Copy cookbook
# Removed since we don't have the cookbok as a submodule present
# ADD --chown=yoctouser:yoctouser cookbook /tmp/cookbook/

USER yoctouser
WORKDIR /home/yoctouser

# Script which allows to pass containers CMD as an argument to timeout command
# in case we need redefine entrypoint '--entrypoint' key can be used durring container start
RUN echo "#!/usr/bin/env bash" >> /home/yoctouser/docker-ep.sh && \
    echo 'exec  timeout --signal=SIGKILL 21600 "$@"' >> /home/yoctouser/docker-ep.sh && \
    chmod +x /home/yoctouser/docker-ep.sh
ENTRYPOINT ["/home/yoctouser/docker-ep.sh"]

USER root

# For libxml-simple-perl
RUN curl -sL0 http://archive.ubuntu.com/ubuntu/pool/main/libx/libxml-simple-perl/libxml-simple-perl_2.24-1_all.deb > libxml-simple-perl_2.24-1_all.deb && sudo dpkg -i libxml-simple-perl_2.24-1_all.deb

# Prerequisites for QC BSP
RUN apt-get install gawk wget git-core diffstat unzip texinfo gcc-multilib build-essential chrpath \
    socat cpio python python3 python3-pip python3-pexpect xz-utils debianutils iputils-ping

RUN apt-get update && \
    apt-get install lsb-release software-properties-common -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    apt-get update && \
    apt-get install openjdk-11-jdk g++-7 g++-7-multilib gcc-7 gcc-7-multilib -y && \
    update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-7 70 --slave /usr/bin/g++ g++ /usr/bin/g++-7 --slave /usr/bin/gcov gcov /usr/bin/gcov-7 && \
    apt-get clean -y

USER yoctouser

RUN git config --global user.email "yoctouser@docker"
RUN git config --global user.name "yoctouser"
