require pulseaudio_13.0.inc

FILESEXTRAPATHS_prepend := "${THISDIR}/pulseaudio:"
FILESPATH =+ "${WORKSPACE}/:"

SRC_URI = "file://external/pulseaudio/ \
           file://0001-client-conf-Add-allow-autospawn-for-root.patch \
           file://0002-do-not-display-CLFAGS-to-improve-reproducibility-bui.patch \
           file://0001-remap-arm-Adjust-inline-asm-constraints.patch \
           file://volatiles.04_pulse \
           file://pulseaudio.service \
           file://system-${BASEMACHINE}.pa \
           file://daemon_conf_in.patch \
           "

S = "${WORKDIR}/external/pulseaudio"

do_compile_prepend() {
	mkdir -p ${S}/libltdl
	cp ${STAGING_LIBDIR}/libltdl* ${S}/libltdl
}

do_configure_prepend() {
	cd ${S}
	NOCONFIGURE=1 ./bootstrap.sh
	cd ${B}
	export GIT_DESCRIBE_FOR_BUILD="${PV}"
}

do_install_append() {
	install -d ${D}${systemd_system_unitdir}
	install -m 0644 ${WORKDIR}/pulseaudio.service ${D}${systemd_system_unitdir}
	install -d ${D}${systemd_system_unitdir}/multi-user.target.wants/
	# enable the service for multi-user.target
	ln -sf ${systemd_system_unitdir}/pulseaudio.service \
	       ${D}${systemd_system_unitdir}/multi-user.target.wants/pulseaudio.service

	if [ ${BASEMACHINE} == "qrb5165" ] ; then
		install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
	fi

        if [ ${BASEMACHINE} == "sxr2130" ] ; then
		install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
        fi
	if [ ${BASEMACHINE} == "neo" ] ; then
		install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
	fi
	if [ ${BASEMACHINE} == "qrbx210" ] ; then
		install -m 0644 ${WORKDIR}/system-${BASEMACHINE}.pa ${D}${sysconfdir}/pulse/system.pa
	fi

	for i in $(find ${S}/src/pulsecore/ -type d -printf "pulsecore/%P\n"); do
		[ -n "$(ls ${S}/src/${i}/*.h 2>/dev/null)" ] || continue
		install -d ${D}${includedir}/${i}
		install -m 0644 ${S}/src/${i}/*.h ${D}${includedir}/${i}/
	done
	# not really the expected path for config.h but we can't just
	# put it ${includedir}, it's too generic a name.
	install -m 0644 ${WORKDIR}/build/config.h ${D}${includedir}/pulsecore
}

GROUPADD_PARAM_pulseaudio-server = "-g 5020 pulse"
USERADD_PARAM_pulseaudio-server = "--system --home /var/run/pulse \
                              --no-create-home --shell /bin/false \
                              --groups audio,pulse,input,plugdev,diag --gid pulse pulse"

SYSTEMD_PACKAGES = "${PN}-server"

PACKAGES =+ "libpulsecore-dev"
FILES_libpulsecore-dev = "${includedir}/pulsecore/*"

# Explicitly create this directory for the volatile bind mount to work
FILES_${PN}-server += "/var/lib/pulse"

# Build the qahw module on qrb5165
DEPENDS_append_qrb5165 = " qahw audiohal"
EXTRA_OECONF_append_qrb5165 += " --with-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OECONF_append_qrb5165 += " --with-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS_pulseaudio-server_append_qrb5165 += " pulseaudio-module-qahw-card"

# Build the qahw module on qrbx210
DEPENDS_append_qrbx210 = " qahw audiohal"
EXTRA_OECONF_append_qrbx210 += " --with-qahw-api=${STAGING_INCDIR}/mm-audio/qahw_api/inc"
EXTRA_OECONF_append_qrbx210 += " --with-qahw=${STAGING_INCDIR}/mm-audio/qahw/inc"
RDEPENDS_pulseaudio-server_append_qrbx210 += " pulseaudio-module-qahw-card"

# Build the qsthw module on qrb5165
DEPENDS_append_qrb5165 = " qsthw qsthw-api"
EXTRA_OECONF_append_qrb5165 += " --with-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS_pulseaudio-server_append_qrb5165 += " pulseaudio-module-qsthw"
RDEPENDS_pulseaudio-server_append_qrb5165 += " pulseaudio-module-dbus-protocol"

# Build the pal module on sxr2130
DEPENDS_append_sxr2130 = " qal"
EXTRA_OECONF_append_sxr2130 += " --with-qal=${STAGING_INCDIR}/pal"
RDEPENDS_pulseaudio-server_append_sxr2130 += " pulseaudio-module-qal-card"
RDEPENDS_pulseaudio-server_append_sxr2130 += " pulseaudio-module-dbus-protocol"

# Build the qal module on neo
DEPENDS_append_neo = " qal"
EXTRA_OECONF_append_neo += " --with-qal=${STAGING_INCDIR}/pal"
RDEPENDS_pulseaudio-server_append_neo += " pulseaudio-module-qal-card pulseaudio-module-qal-voiceui-card"
RDEPENDS_pulseaudio-server_append_neo += " pulseaudio-module-dbus-protocol"

# Build the qsthw module on qrbx210
DEPENDS_append_qrbx210 = " qsthw qsthw-api"
EXTRA_OECONF_append_qrbx210 += " --with-qsthw=${STAGING_INCDIR}/mm-audio/qsthw_api"
RDEPENDS_pulseaudio-server_append_qrbx210 += " pulseaudio-module-qsthw"
RDEPENDS_pulseaudio-server_append_qrbx210 += " pulseaudio-module-dbus-protocol"

FILES_${PN}-module-qahw-card += "${datadir}/pulseaudio/qahw"
FILES_${PN}-module-qal-card += "${datadir}/pulseaudio/qal"
FILES_${PN} = "${datadir}/* ${libdir}/* ${sysconfdir}/* ${bindir}/* ${base_libdir}/* ${prefix}/libexec/"
