SUMMARY = "QTI PulseAudio opensource package groups"
LICENSE = "BSD-3-Clause-Clear"

inherit packagegroup

PROVIDES = "${PACKAGES}"

PACKAGES = ' \
    ${@bb.utils.contains("DISTRO_FEATURES", "pulseaudio", bb.utils.contains("DISTRO_FEATURES", "qti-audio", "packagegroup-qti-pulseaudio", "", d), "", d)} \
'

RDEPENDS:packagegroup-qti-pulseaudio = ' \
    pulseaudio-server \
    pulseaudio-misc \
    pulseaudio-module-combine-sink \
    pulseaudio-module-loopback \
    pulseaudio-module-null-source \
    pulseaudio-module-role-cork \
    pulseaudio-module-role-exclusive \
    pulseaudio-module-role-ignore \
    pulseaudio-module-switch-on-port-available \
'
