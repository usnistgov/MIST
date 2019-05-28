FROM nvidia/cuda:9.0-runtime-ubuntu16.04
MAINTAINER National Institute of Standards and Technology

ARG EXEC_DIR="/opt/executables"
ARG DATA_DIR="/data"

# Create folders
RUN mkdir -p ${EXEC_DIR} \
    && mkdir -p ${EXEC_DIR}/lib/jcuda \
    && mkdir -p ${DATA_DIR}/inputs \
    && mkdir ${DATA_DIR}/outputs
    
# Install fftw and java 8 jdk
RUN apt-get update \
    && apt-get install -y libfftw3-dev \
    && apt-get install -y openjdk-8-jdk \
    && update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# Copy MIST CUDA lib
COPY lib/jcuda/*.ptx ${EXEC_DIR}/lib/jcuda/

# Copy MIST JAR
COPY target/MIST_*-jar-with-dependencies.jar ${EXEC_DIR}/MIST.jar

# Set working directory
WORKDIR ${EXEC_DIR}

# Default command. Additional arguments are provided through the command line
ENTRYPOINT ["/usr/bin/java", "-jar", "MIST.jar"]