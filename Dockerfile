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
COPY . $HOME
RUN chown -R $NB_UID $HOME

USER $NB_USER
ENV PATH "$HOME/.local/bin:$PATH"
RUN pip install --no-cache --user JPype1 jupyter build wheel
WORKDIR $HOME/optapy-core
RUN python -m build
RUN pip install --no-cache --user dist/optapy-0.0.0-py3-none-any.whl
WORKDIR $HOME/notebook

CMD ["jupyter", "notebook", "--ip", "0.0.0.0"]