require pulseaudio.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/pulseaudio:"
FILESPATH =+ "${WORKSPACE}/:"

SRC_URI = "file://external/pulseaudio/ \
           file://0001-client-conf-Add-allow-autospawn-for-root.patch \
           file://0002-do-not-display-CLFAGS-to-improve-reproducibility-bui.patch \
           file://0001-meson-Check-for-__get_cpuid.patch \
           file://volatiles.04_pulse \
           file://0001-doxygen-meson.build-remove-dependency-on-doxygen-bin.patch \
           file://pulseaudio.service \
           file://daemon_conf_in.patch \
           file://99-pasthru_adsp.rules \
           "
SRC_URI:append = " ${@bb.utils.contains('BASEMACHINE', 'qcm4325-mtp', 'file://system-qcm2290-mtp.pa', 'file://system-${BASEMACHINE}.pa', d)}"
AR_PULSEAUDIO_SERVICE_MACHINES = "kalama pineapple sun qcm2290-mtp qcm4325-mtp kera sdmsteppe"
SRC_URI:append = " ${@bb.utils.contains_any('BASEMACHINE', d.getVar('AR_PULSEAUDIO_SERVICE_MACHINES'), 'file://ar-pulseaudio.service', '', d)}"

S = "${WORKDIR}/external/pulseaudio"

do_compile:prepend() {
    mkdir -p ${S}/libltdl
    cp ${STAGING_LIBDIR}/libltdl* ${S}/libltdl
}

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/pulseaudio.service ${D}${systemd_system_unitdir}
    install -d ${D}${systemd_system_unitdir}/multi-user.target.wants/
    # enable the service for multi-user.target
    ln -sf ${systemd_system_unitdir}/pulseaudio.service \
        ${D}${systemd_system_unitdir}/multi-user.target.wants/pulseaudio.service

    case "${BASEMACHINE}" in
        "qrb5165"|"sxr2130"|"neo"|"qrbx210")
            install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
            ;;
    esac
    case "${BASEMACHINE}" in
        "kalama"|"pineapple"|"sun"|"kera")
            install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
            install -m 0644 ${WORKDIR}/ar-pulseaudio.service ${D}${systemd_system_unitdir}/pulseaudio.service
            ;;
    esac
    case "${BASEMACHINE}" in
        "qcm2290-mtp")
            install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
            install -m 0644 ${WORKDIR}/ar-pulseaudio.service ${D}${systemd_system_unitdir}/pulseaudio.service
            install -d ${D}${libdir}/udev/rules.d
            install -m 0644 ${WORKDIR}/99-pasthru_adsp.rules ${D}${libdir}/udev/rules.d/99-pasthru_adsp.rules
            ;;
    esac
    case "${BASEMACHINE}" in
        "qcm4325-mtp")
            install -m 0644 ${WORKDIR}/system-qcm2290-mtp.pa ${D}${sysconfdir}/pulse/system.pa
            install -m 0644 ${WORKDIR}/ar-pulseaudio.service ${D}${systemd_system_unitdir}/pulseaudio.service
            install -d ${D}${libdir}/udev/rules.d
            install -m 0644 ${WORKDIR}/99-pasthru_adsp.rules ${D}${libdir}/udev/rules.d/99-pasthru_adsp.rules
            ;;
    esac
    case "${BASEMACHINE}" in
        "sdmsteppe")
                install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
                install -m 0644 ${WORKDIR}/ar-pulseaudio.service ${D}${systemd_system_unitdir}/pulseaudio.service
                sed -i '/^\[Service\]/i# Prevent starting if node is not available\nConditionPathExists=/sys/kernel/boot_adsp/boot\n' ${D}${systemd_system_unitdir}/pulseaudio.service
            ;;
    esac

    for i in $(find ${S}/src/pulsecore/ -type d -printf "pulsecore/%P\n"); do
        [ -n "$(ls ${S}/src/${i}/*.h 2>/dev/null)" ] || continue
        install -d ${D}${includedir}/${i}
        install -m 0644 ${S}/src/${i}/*.h ${D}${includedir}/${i}/
    done
    # not really the expected path for config.h but we can't just
    # put it ${includedir}, it's too generic a name.
    install -m 0644 ${WORKDIR}/build/config.h ${D}${includedir}/pulsecore
}

GROUPADD_PARAM:pulseaudio-server = "-g 5020 pulse"
USERADD_PARAM:pulseaudio-server = "-u 1000 -U system; --system --home /var/run/pulse \
                              --no-create-home --shell /bin/false \
                              --groups audio,pulse,input,plugdev,diag,system --gid pulse pulse"

SYSTEMD_PACKAGES = "${PN}-server"

PACKAGES =+ "libpulsecore-dev"
FILES_libpulsecore-dev = "${includedir}/pulsecore/*"

# Explicitly create this directory for the volatile bind mount to work
FILES:${PN}-server += "/var/lib/pulse"

FILES:${PN} += "${libdir}/udev/rules.d/99-pasthru_adsp.rules"

EXTRA_OEMESON:append = " -Dpa_version=15.0"

# Build the qahw module on qrb5165
DEPENDS:append:qrb5165 = " qahw audiohal"
EXTRA_OEMESON:append:qrb5165 = " -Dwith-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OEMESON:append:qrb5165 = " -Dwith-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-qahw-card"

# Build the qahw module on qrbx210
DEPENDS:append:qrbx210 = " qahw audiohal"
EXTRA_OEMESON:append:qrbx210 = " -Dwith-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OEMESON:append:qrbx210 = " -Dwith-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-qahw-card"

# Build the qsthw module on qrb5165
DEPENDS:append:qrb5165 = " qsthw qsthw-api"
EXTRA_OEMESON:append:qrb5165 = " -Dwith-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-qsthw"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-dbus-protocol"

# Build the pal module on sxr2130
DEPENDS:append:sxr2130 = " qal"
EXTRA_OEMESON:append:sxr2130 = " -Dwith-qal=${STAGING_INCDIR}/pal"
RDEPENDS:pulseaudio-server:append:sxr2130 = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:sxr2130 = " pulseaudio-module-dbus-protocol"

# Build the qal module on neo
DEPENDS:append:neo = " qal"
EXTRA_OEMESON:append:neo = " -Dwith-qal=${STAGING_INCDIR}/pal"
RDEPENDS:pulseaudio-server:append:neo = " pulseaudio-module-qal-card pulseaudio-module-qal-voiceui-card"
RDEPENDS:pulseaudio-server:append:neo = " pulseaudio-module-dbus-protocol"

# Build the qsthw module on qrbx210
DEPENDS:append:qrbx210 = " qsthw qsthw-api"
EXTRA_OEMESON:append:qrbx210 = " -Dwith-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-qsthw"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-dbus-protocol"

# Build the qal module on kalama
DEPENDS:append:kalama = " qal palserver"
EXTRA_OEMESON:append:kalama = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:kalama = " -Denable-pal-service=yes"
RDEPENDS:pulseaudio-server:append:kalama = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:kalama = " pulseaudio-module-dbus-protocol"

# Build the qal voiceui card on kalama
EXTRA_OEMESON:append:kalama = " -Dwith-qal-voiceui=${STAGING_INCDIR}/pal"
RDEPENDS:pulseaudio-server:append:kalama = " pulseaudio-module-qal-voiceui-card"

# Build the qal module on pineapple
DEPENDS:append:pineapple = " qal palserver"
EXTRA_OEMESON:append:pineapple = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:pineapple = " -Denable-pal-service=yes"
RDEPENDS:pulseaudio-server:append:pineapple = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:pineapple = " pulseaudio-module-dbus-protocol"

# Build the qal module on sun
DEPENDS:append:sun = " qal palserver"
EXTRA_OEMESON:append:sun = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:sun = " -Denable-pal-service=yes"
EXTRA_OEMESON:append:sun = " -Dwith-refactored-pal=true"
RDEPENDS:pulseaudio-server:append:sun = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:sun = " pulseaudio-module-dbus-protocol"
GROUPADD_PARAM:pulseaudio-server:remove:sun = "-g 5020 pulse"

# Build the qal module on qcm2290-mtp
DEPENDS:append:qcm2290-mtp = " qal palserver"
EXTRA_OEMESON:append:qcm2290-mtp = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:qcm2290-mtp = " -Denable-pal-service=yes"
EXTRA_OEMESON:append:qcm2290-mtp = " -Dpal-support-card-status=false"
RDEPENDS:pulseaudio-server:append:qcm2290-mtp = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:qcm2290-mtp = " pulseaudio-module-dbus-protocol"

# Build the qal module on qcm4325-mtp
DEPENDS:append:qcm4325-mtp = " qal palserver"
EXTRA_OEMESON:append:qcm4325-mtp = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:qcm4325-mtp = " -Denable-pal-service=yes"
EXTRA_OEMESON:append:qcm4325-mtp = " -Dpal-support-card-status=false"
RDEPENDS:pulseaudio-server:append:qcm4325-mtp = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:qcm4325-mtp = " pulseaudio-module-dbus-protocol"

# Build the qal module on kera
DEPENDS:append:kera = " qal palserver"
EXTRA_OEMESON:append:kera = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:kera = " -Dwith-refactored-pal=true"
EXTRA_OEMESON:append:kera = " -Denable-pal-service=yes"
RDEPENDS:pulseaudio-server:append:kera = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:kera = " pulseaudio-module-dbus-protocol"
GROUPADD_PARAM:pulseaudio-server:remove:kera = "-g 5020 pulse"

# Build the qal module on sdmsteppe
DEPENDS:append:sdmsteppe = " qal palserver"
EXTRA_OEMESON:append:sdmsteppe = " -Dwith-qal=${STAGING_INCDIR}/pal"
EXTRA_OEMESON:append:sdmsteppe = " -Denable-pal-service=yes"
EXTRA_OEMESON:append:sdmsteppe = " -Dwith-refactored-pal=true"
RDEPENDS:pulseaudio-server:append:sdmsteppe = " pulseaudio-module-qal-card"
RDEPENDS:pulseaudio-server:append:sdmsteppe = " pulseaudio-module-dbus-protocol"
GROUPADD_PARAM:pulseaudio-server:remove:sdmsteppe = "-g 5020 pulse"

FILES:${PN}-module-qahw-card += "${datadir}/pulseaudio/qahw"
FILES:${PN}-module-qal-card += "${datadir}/pulseaudio/qal"
FILES:${PN} = "${datadir}/* ${libdir}/* ${sysconfdir}/* ${bindir}/* ${base_libdir}/* ${prefix}/libexec/"
