FROM python:3.9.6-slim-buster

# work-around on Debian JDK install bug
RUN mkdir -p /usr/share/man/man1
RUN apt-get update
RUN apt-get install -y openjdk-11-jdk-headless

ENV NB_USER jovyan
ENV NB_UID 1000
ENV HOME /home/$NB_USER

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid $NB_UID \
    $NB_USER

USER root
COPY notebook $HOME/notebook
RUN chown -R $NB_UID $HOME

USER $NB_USER
ENV PATH "$HOME/.local/bin:$PATH"
RUN pip install --no-cache --user optapy jupyterlab build wheel
WORKDIR $HOME/notebook

CMD ["jupyter", "lab", "--ip", "0.0.0.0"]