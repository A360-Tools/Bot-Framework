# Cross-compiles a slim static ffmpeg.exe for win64 with libx264 (FAST mode)
# and libaom-av1 (COMPACT mode), plus the minimal set of muxers, demuxers,
# parsers, filters, and bitstream filters the recorder actually uses. Output
# is a single static binary (~12-15 MB).
#
# Build:    docker build -t a360-ffmpeg-builder tools/ffmpeg-build
# Extract:  docker run --rm -v <out>:/out a360-ffmpeg-builder
#           -> ffmpeg.exe lands at <out>/ffmpeg.exe
#
# The encoders we explicitly enable: libx264, libaom-av1, ffvhuff.
# All other encoders/decoders/filters/muxers/demuxers/parsers/bsfs default off.

FROM debian:bookworm-slim AS builder

ENV DEBIAN_FRONTEND=noninteractive
ENV TARGET=x86_64-w64-mingw32
ENV PREFIX=/cross
ENV PKG_CONFIG_PATH=$PREFIX/lib/pkgconfig
ENV PATH=$PREFIX/bin:$PATH

# Pinned versions so reproducibility doesn't drift on rebuilds.
ENV X264_REV=baee400fa9ced6f5481a728138fed6e867b0ff7f
ENV AOM_TAG=v3.10.0
ENV FFMPEG_TAG=n6.0

RUN apt-get update && apt-get install -y --no-install-recommends \
        autoconf automake build-essential cmake git nasm pkg-config \
        mingw-w64 mingw-w64-tools libtool yasm make \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# x264 (GPL) -> static lib for cross
RUN git clone https://code.videolan.org/videolan/x264.git x264 \
    && cd x264 \
    && git checkout $X264_REV \
    && ./configure \
         --prefix=$PREFIX \
         --host=$TARGET \
         --cross-prefix=$TARGET- \
         --enable-static \
         --disable-cli \
         --disable-opencl \
         --enable-pic \
    && make -j"$(nproc)" \
    && make install

# libaom (BSD) -> static lib for cross
RUN git clone https://aomedia.googlesource.com/aom aom \
    && cd aom \
    && git checkout $AOM_TAG \
    && cd /src && mkdir aom-build && cd aom-build \
    && cmake ../aom \
         -DCMAKE_TOOLCHAIN_FILE=../aom/build/cmake/toolchains/x86_64-mingw-gcc.cmake \
         -DCMAKE_INSTALL_PREFIX=$PREFIX \
         -DBUILD_SHARED_LIBS=OFF \
         -DENABLE_TESTS=OFF \
         -DENABLE_TOOLS=OFF \
         -DENABLE_DOCS=OFF \
         -DENABLE_EXAMPLES=OFF \
         -DCONFIG_AV1_DECODER=1 \
         -DCONFIG_AV1_ENCODER=1 \
    && make -j"$(nproc)" \
    && make install

# ffmpeg (GPL'd because of x264). Disables every encoder, decoder, muxer,
# demuxer, parser, filter, and bitstream filter by default and individually
# re-enables only what the recorder pipelines need.
RUN git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg \
    && cd ffmpeg \
    && git checkout $FFMPEG_TAG \
    && ./configure \
         --prefix=$PREFIX \
         --target-os=mingw32 \
         --arch=x86_64 \
         --cross-prefix=$TARGET- \
         --pkg-config=pkg-config \
         --pkg-config-flags=--static \
         --extra-ldexeflags=-static \
         --extra-cflags="-I$PREFIX/include" \
         --extra-ldflags="-L$PREFIX/lib" \
         --enable-cross-compile \
         --enable-gpl \
         --disable-encoders --disable-decoders --disable-parsers \
         --disable-filters --disable-muxers --disable-demuxers --disable-bsfs \
         --enable-libx264 --enable-encoder=libx264 \
         --enable-libaom --enable-encoder=libaom_av1 --enable-decoder=libaom_av1 \
         --enable-encoder=ffvhuff --enable-decoder=ffvhuff \
         --enable-parser=av1 --enable-parser=h264 \
         --enable-bsf=av1_frame_merge --enable-bsf=av1_frame_split --enable-bsf=av1_metadata \
         --enable-bsf=h264_mp4toannexb \
         --enable-filter=segment --enable-filter=framerate --enable-filter=scale \
         --enable-swscale --enable-swscale-alpha \
         --enable-muxer=segment --enable-muxer=mp4 \
         --enable-demuxer=concat --enable-demuxer=mov \
         --enable-static --disable-shared \
         --disable-w32threads --disable-autodetect \
         --disable-doc --disable-htmlpages --disable-manpages \
         --disable-podpages --disable-txtpages \
         --disable-ffplay --disable-ffprobe \
    && make -j"$(nproc)"

# Stage 2: minimal runtime image whose default action is to publish the
# compiled binary into a bind-mounted /out volume.
FROM debian:bookworm-slim
COPY --from=builder /src/ffmpeg/ffmpeg.exe /ffmpeg.exe
RUN apt-get update && apt-get install -y --no-install-recommends file \
    && rm -rf /var/lib/apt/lists/*
ENTRYPOINT ["sh", "-c", "cp /ffmpeg.exe /out/ffmpeg.exe && file /out/ffmpeg.exe && ls -la /out/ffmpeg.exe"]
