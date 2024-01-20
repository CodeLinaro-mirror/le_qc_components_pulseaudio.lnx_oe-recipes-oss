require pulseaudio_13.0.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/pulseaudio:"
FILESPATH =+ "${WORKSPACE}/:"

SRC_URI = "file://external/pulseaudio/ \
           file://0001-client-conf-Add-allow-autospawn-for-root.patch \
           file://0002-do-not-display-CLFAGS-to-improve-reproducibility-bui.patch \
           file://0001-remap-arm-Adjust-inline-asm-constraints.patch \
           file://volatiles.04_pulse \
           file://pulseaudio.service \
           file://daemon_conf_in.patch \
           file://${BASEMACHINE}/ \
           "

S = "${WORKDIR}/external/pulseaudio"

do_compile:prepend() {
	mkdir -p ${S}/libltdl
	cp ${STAGING_LIBDIR}/libltdl* ${S}/libltdl
}

do_configure:prepend() {
	cd ${S}
	NOCONFIGURE=1 ./bootstrap.sh
	cd ${B}
	export GIT_DESCRIBE_FOR_BUILD="${PV}"
}

do_install:append() {
	install -d ${D}${systemd_system_unitdir}
	install -d ${D}${systemd_system_unitdir}/multi-user.target.wants/

	if [ ${BASEMACHINE} == "neo" ] ; then
		install -m 0644 ${WORKDIR}/${BASEMACHINE}/pulseaudio.service ${D}${systemd_system_unitdir}
	else
		install -m 0644 ${WORKDIR}/pulseaudio.service ${D}${systemd_system_unitdir}
	fi

	# enable the service for multi-user.target
	ln -sf ${systemd_system_unitdir}/pulseaudio.service \
		   ${D}${systemd_system_unitdir}/multi-user.target.wants/pulseaudio.service

	install -m 0644 ${WORKDIR}/${BASEMACHINE}/system.pa ${D}${sysconfdir}/pulse/system.pa

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
USERADD_PARAM:pulseaudio-server = "--system --home /var/run/pulse \
                              --no-create-home --shell /bin/false \
                              --groups audio,pulse,input,plugdev,diag --gid pulse pulse"

SYSTEMD_PACKAGES = "${PN}-server"

PACKAGES =+ "libpulsecore-dev"
FILES:libpulsecore-dev = "${includedir}/pulsecore/*"

# Explicitly create this directory for the volatile bind mount to work
FILES:${PN}-server += "/var/lib/pulse"

# Build the qahw module on qrb5165
DEPENDS:append:qrb5165 = " qahw audiohal"
EXTRA_OECONF:append:qrb5165 = " --with-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OECONF:append:qrb5165 = " --with-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-qahw-card"

# Build the qahw module on qrbx210
DEPENDS:append:qrbx210 = " qahw audiohal"
EXTRA_OECONF:append:qrbx210 = " --with-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OECONF:append:qrbx210 = " --with-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-qahw-card"

# Build the qsthw module on qrb5165
DEPENDS:append:qrb5165 = " qsthw qsthw-api"
EXTRA_OECONF:append:qrb5165 = " --with-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-qsthw"
RDEPENDS:pulseaudio-server:append:qrb5165 = " pulseaudio-module-dbus-protocol"

# Build the pal module on sxr2130
DEPENDS:append:sxr2130 = " pal"
EXTRA_OECONF:append:sxr2130 = " --with-pal=${STAGING_INCDIR}/pal"
RDEPENDS:pulseaudio-server:append:sxr2130 = " pulseaudio-module-pal-card"
RDEPENDS:pulseaudio-server:append:sxr2130 = " pulseaudio-module-dbus-protocol"

# Build the pal module on neo
DEPENDS:append:neo = " pal"
EXTRA_OECONF:append:neo = " --with-pal=${STAGING_INCDIR}/pal"
RDEPENDS:pulseaudio-server:append:neo = " pulseaudio-module-pal-card pulseaudio-module-pal-voiceui-card"
RDEPENDS:pulseaudio-server:append:neo = " pulseaudio-module-dbus-protocol"

# Build the qsthw module on qrbx210
DEPENDS:append:qrbx210 = " qsthw qsthw-api"
EXTRA_OECONF:append:qrbx210 = " --with-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-qsthw"
RDEPENDS:pulseaudio-server:append:qrbx210 = " pulseaudio-module-dbus-protocol"

# Build the qahw module on qcs6490
DEPENDS:append:qcs6490 = " qahw audiohal"
EXTRA_OECONF:append:qcs6490 = " --with-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OECONF:append:qcs6490 = " --with-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS:pulseaudio-server:append:qcs6490 = " pulseaudio-module-qahw-card"

# Build the qsthw module on qcs6490
DEPENDS:append:qcs6490 = " qsthw qsthw-api"
EXTRA_OECONF:append:qcs6490 = " --with-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS:pulseaudio-server:append:qcs6490 = " pulseaudio-module-qsthw"
RDEPENDS:pulseaudio-server:append:qcs6490 = " pulseaudio-module-dbus-protocol"

FILES:${PN}-module-qahw-card += "${datadir}/pulseaudio/qahw"
FILES:${PN}-module-pal-card += "${datadir}/pulseaudio/pal"
FILES:${PN} = "${datadir}/* ${libdir}/* ${sysconfdir}/* ${bindir}/* ${base_libdir}/* ${prefix}/libexec/"
