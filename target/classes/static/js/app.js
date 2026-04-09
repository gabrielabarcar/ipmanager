const sortStates = {};
let sessionWarningModalInstance = null;
let sessionWarningTimeout = null;
let sessionLogoutTimeout = null;

let currentPage = 1;
let rowsPerPage = 50;
let onlyConflictMode = false;

const validationState = {
    create: {
        enlaceValido: true,
        lanValida: true,
        wanValida: true,
        lanTieneTraslape: false,
        wanTieneTraslape: false
    },
    modal: {
        enlaceValido: true,
        lanValida: true,
        wanValida: true,
        lanTieneTraslape: false,
        wanTieneTraslape: false
    }
};

document.addEventListener("DOMContentLoaded", function () {
    document.body.classList.add("sidebar-collapsed");

    insertarBloqueTraslapeCrear();

    configurarBusqueda();
    configurarFormulario("create");
    configurarFormulario("modal");
    configurarAutocompletadoSugerido("create");
    configurarAutocompletadoSugerido("modal");
    configurarRefrescoFormularioCrear();
    configurarDecisionTraslape("create");
    configurarDecisionTraslape("modal");
    configurarSesionInactiva();
    configurarBotonCerrarFormulario();
    configurarPaginacion();
    configurarFiltroConflictos();
    configurarGuardarYCompartirCorreo();

    aplicarMascarasPorDefecto("create");
    aplicarMascarasPorDefecto("modal");
    cargarSugerenciasIp("create");
    aplicarFiltrosTabla();
    actualizarEstadoGuardar("create");
    actualizarEstadoGuardar("modal");
    procesarCorreoPostGuardado();
    configurarCalculadoraArrastrable();
});

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "X-CSRF-TOKEN";
}

async function postJson(url, payload) {
    return fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [getCsrfHeader()]: getCsrfToken()
        },
        body: JSON.stringify(payload)
    });
}

async function getJson(url) {
    return fetch(url, {
        method: "GET",
        headers: {
            [getCsrfHeader()]: getCsrfToken()
        }
    });
}

function getContextContainer(context) {
    if (context === "create") {
        return document.querySelector("#simpleCreateForm form");
    }
    if (context === "modal") {
        return document.querySelector("#registroModal");
    }
    return null;
}

function getScopedElement(context, id) {
    const container = getContextContainer(context);
    if (!container) return null;
    return container.querySelector(`#${id}`);
}

function getCreateForm() {
    return document.querySelector("#simpleCreateForm form");
}

function getModalElement() {
    return document.getElementById("registroModal");
}

function getModalForm() {
    const modal = getModalElement();
    return modal ? modal.querySelector("form") : null;
}

function configurarBusqueda() {
    const globalInput = document.getElementById("searchInputGlobal");
    const columnInputs = document.querySelectorAll(".column-filter");

    if (globalInput) {
        globalInput.addEventListener("input", () => {
            currentPage = 1;
            aplicarFiltrosTabla();
        });
    }

    columnInputs.forEach(input => {
        input.addEventListener("input", () => {
            currentPage = 1;
            aplicarFiltrosTabla();
        });
    });
}

function configurarFiltroConflictos() {
    const conflictCard = document.getElementById("conflictSummaryCard");
    const btnVerTodos = document.getElementById("btnVerTodosRegistros");

    if (conflictCard) {
        conflictCard.addEventListener("click", function () {
            onlyConflictMode = true;
            currentPage = 1;
            aplicarFiltrosTabla();
        });
    }

    if (btnVerTodos) {
        btnVerTodos.addEventListener("click", function () {
            onlyConflictMode = false;
            limpiarTodosLosFiltros();
            currentPage = 1;
            aplicarFiltrosTabla();
        });
    }
}

function limpiarTodosLosFiltros() {
    const globalInput = document.getElementById("searchInputGlobal");
    const columnInputs = document.querySelectorAll(".column-filter");

    if (globalInput) globalInput.value = "";
    columnInputs.forEach(input => input.value = "");
}

function limpiarInput(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.value = "";
    currentPage = 1;
    aplicarFiltrosTabla();
    input.focus();
}

function limpiarFiltroColumna(button) {
    const wrapper = button.closest(".input-group");
    if (!wrapper) return;

    const input = wrapper.querySelector(".column-filter");
    if (!input) return;

    input.value = "";
    currentPage = 1;
    aplicarFiltrosTabla();
    input.focus();
}

function aplicarFiltrosTabla() {
    const table = document.getElementById("networksTable");
    if (!table) return;

    const tbodyRows = Array.from(table.querySelectorAll("tbody tr"));
    const globalValue = (document.getElementById("searchInputGlobal")?.value || "").trim().toLowerCase();
    const columnInputs = document.querySelectorAll(".column-filter");

    const filteredRows = tbodyRows.filter(row => {
        let visible = true;

        if (onlyConflictMode) {
            const conflicto = (row.dataset.conflicto || "").toLowerCase() === "true";
            if (!conflicto) {
                visible = false;
            }
        }

        if (visible && globalValue) {
            const rowText = row.innerText.toLowerCase();
            const globalTextMatch = rowText.includes(globalValue);
            const globalOverlapMatch = buscarTraslapeEnFila(row, globalValue, "both");

            if (!(globalTextMatch || globalOverlapMatch)) {
                visible = false;
            }
        }

        if (visible) {
            for (const input of columnInputs) {
                const value = input.value.trim().toLowerCase();
                if (!value) continue;

                const colIndex = parseInt(input.dataset.col, 10);
                const filterType = input.dataset.type || "text";
                const cellText = (row.cells[colIndex]?.innerText || "").toLowerCase();

                let match = false;

                if (filterType === "lan") {
                    match = cellText.includes(value) || buscarTraslapeEnFila(row, value, "lan");
                } else if (filterType === "wan") {
                    match = cellText.includes(value) || buscarTraslapeEnFila(row, value, "wan");
                } else {
                    match = cellText.includes(value);
                }

                if (!match) {
                    visible = false;
                    break;
                }
            }
        }

        return visible;
    });

    renderPaginatedRows(tbodyRows, filteredRows);
}

function renderPaginatedRows(allRows, filteredRows) {
    const totalRows = filteredRows.length;
    const totalPages = Math.max(1, Math.ceil(totalRows / rowsPerPage));

    if (currentPage > totalPages) {
        currentPage = totalPages;
    }

    const start = (currentPage - 1) * rowsPerPage;
    const end = start + rowsPerPage;

    allRows.forEach(row => {
        row.style.display = "none";
    });

    filteredRows.slice(start, end).forEach(row => {
        row.style.display = "";
    });

    renderPaginationControls(totalRows, totalPages, start, end);
}

function renderPaginationControls(totalRows, totalPages, start, end) {
    const paginationTop = document.getElementById("paginationTop");
    const paginationBottom = document.getElementById("paginationBottom");
    const infoTop = document.getElementById("tablePageInfoTop");
    const infoBottom = document.getElementById("tablePageInfoBottom");

    const from = totalRows === 0 ? 0 : start + 1;
    const to = Math.min(end, totalRows);

    let infoText = `Mostrando ${from} a ${to} de ${totalRows} registros`;
    if (onlyConflictMode) {
        infoText += " (solo conflictos)";
    }

    if (infoTop) infoTop.textContent = infoText;
    if (infoBottom) infoBottom.textContent = infoText;

    const html = buildPaginationHtml(totalPages);

    if (paginationTop) {
        paginationTop.innerHTML = html;
        bindPaginationEvents(paginationTop, totalPages);
    }

    if (paginationBottom) {
        paginationBottom.innerHTML = html;
        bindPaginationEvents(paginationBottom, totalPages);
    }
}

function buildPaginationHtml(totalPages) {
    let html = "";

    html += `
        <li class="page-item ${currentPage === 1 ? "disabled" : ""}">
            <a class="page-link" href="#" data-page="${currentPage - 1}">Anterior</a>
        </li>
    `;

    for (let i = 1; i <= totalPages; i++) {
        html += `
            <li class="page-item ${i === currentPage ? "active" : ""}">
                <a class="page-link" href="#" data-page="${i}">${i}</a>
            </li>
        `;
    }

    html += `
        <li class="page-item ${currentPage === totalPages ? "disabled" : ""}">
            <a class="page-link" href="#" data-page="${currentPage + 1}">Siguiente</a>
        </li>
    `;

    return html;
}

function bindPaginationEvents(container, totalPages) {
    const links = container.querySelectorAll("a.page-link");
    links.forEach(link => {
        link.addEventListener("click", function (e) {
            e.preventDefault();
            const page = parseInt(this.dataset.page, 10);
            if (isNaN(page) || page < 1 || page > totalPages) return;
            currentPage = page;
            aplicarFiltrosTabla();
        });
    });
}

function configurarPaginacion() {
    const topSelector = document.getElementById("pageSizeSelectorTop");
    const bottomSelector = document.getElementById("pageSizeSelectorBottom");

    const syncPageSize = value => {
        rowsPerPage = parseInt(value, 10) || 50;
        currentPage = 1;

        if (topSelector) topSelector.value = String(rowsPerPage);
        if (bottomSelector) bottomSelector.value = String(rowsPerPage);

        aplicarFiltrosTabla();
    };

    if (topSelector) {
        topSelector.addEventListener("change", function () {
            syncPageSize(this.value);
        });
    }

    if (bottomSelector) {
        bottomSelector.addEventListener("change", function () {
            syncPageSize(this.value);
        });
    }
}

function buscarTraslapeEnFila(row, query, mode) {
    if (!esIpCompleta(query)) {
        return false;
    }

    if (mode === "lan" || mode === "both") {
        const lanText = (row.cells[3]?.innerText || "").trim();
        const lan = separarIpCidr(lanText);
        if (lan && lan.ip !== "0.0.0.0" && ipDentroDeRed(query, lan.ip, lan.cidr)) {
            return true;
        }
    }

    if (mode === "wan" || mode === "both") {
        const wanText = (row.cells[4]?.innerText || "").trim();
        const wan = separarIpCidr(wanText);
        if (wan && wan.ip !== "0.0.0.0" && ipDentroDeRed(query, wan.ip, wan.cidr)) {
            return true;
        }
    }

    return false;
}

function ipDentroDeRed(ipBuscada, redIp, cidr) {
    try {
        const ipLong = ipToLong(ipBuscada);
        const network = networkAddress(redIp, cidr);
        const broadcast = broadcastAddress(network, cidr);
        return ipLong >= network && ipLong <= broadcast;
    } catch (e) {
        return false;
    }
}

function insertarBloqueTraslapeCrear() {
    const form = getCreateForm();
    if (!form) return;
    if (document.getElementById("createTraslapeDecisionBlock")) return;

    const row = form.querySelector(".row.g-3");
    if (!row) return;

    const block = document.createElement("div");
    block.className = "col-12 d-none";
    block.id = "createTraslapeDecisionBlock";

    block.innerHTML = `
        <div class="border rounded p-3 bg-light">
            <div class="form-check mb-2">
                <input class="form-check-input" type="checkbox" name="aceptaTraslape" id="createAceptaTraslape" value="true">
                <label class="form-check-label fw-semibold" for="createAceptaTraslape" id="createTraslapeDecisionLabel">
                    Deseo continuar con el registro aunque exista traslape.
                </label>
            </div>

            <div class="d-none" id="createJustificacionBlock">
                <label class="form-label">Justificación del traslape</label>
                <textarea class="form-control"
                          name="motivoJustificacion"
                          id="createMotivoJustificacion"
                          rows="3"
                          placeholder="Indique por qué se autoriza guardar este registro con traslape."></textarea>
                <div id="createJustificacionFeedback" class="live-feedback"></div>
            </div>
        </div>
    `;

    const saveCol = row.querySelector(".col-12");
    if (saveCol) {
        saveCol.before(block);
    } else {
        row.appendChild(block);
    }
}

function configurarFormulario(context) {
    const formEnlace = getScopedElement(context, "formEnlace");
    const formNetworkIp = getScopedElement(context, "formNetworkIp");
    const formCidr = getScopedElement(context, "formCidr");
    const formWanIp = getScopedElement(context, "formWanIp");
    const formWanCidr = getScopedElement(context, "formWanCidr");

    if (formEnlace) {
        formEnlace.addEventListener("input", function () {
            this.value = this.value.replace(/\D/g, "").slice(0, 8);

            if (this.value.length === 8) {
                validarEnlaceTiempoReal(context);
            } else {
                mostrarFeedbackContext(context, "enlaceFeedback", "", "");
                validationState[context].enlaceValido = true;
                actualizarEstadoGuardar(context);
            }
        });
    }

    if (formNetworkIp) {
        formNetworkIp.addEventListener("input", function () {
            validarLanTiempoReal(context);
        });
    }

    if (formCidr) {
        formCidr.addEventListener("input", function () {
            limitarMascara(this);
            validarLanTiempoReal(context);
        });
    }

    if (formWanIp) {
        formWanIp.addEventListener("input", function () {
            autocompletarWanVisual(context);
            validarWanTiempoReal(context);
        });
    }

    if (formWanCidr) {
        formWanCidr.addEventListener("input", function () {
            limitarMascara(this);
            validarWanTiempoReal(context);
        });
    }
}

function configurarAutocompletadoSugerido(context) {
    const formNetworkIp = getScopedElement(context, "formNetworkIp");
    const formWanIp = getScopedElement(context, "formWanIp");

    [formNetworkIp, formWanIp].forEach(input => {
        if (!input) return;

        input.addEventListener("keydown", function (event) {
            if (event.key !== "Enter") return;

            if (!this.value || this.value.trim() === "") {
                const sugerencia = this.getAttribute("placeholder") || "";
                if (sugerencia.trim() !== "") {
                    this.value = sugerencia.trim();
                    if (this.id === "formNetworkIp") {
                        validarLanTiempoReal(context);
                    } else if (this.id === "formWanIp") {
                        autocompletarWanVisual(context);
                        validarWanTiempoReal(context);
                    }
                }
            }

            event.preventDefault();
        });

        input.addEventListener("blur", function () {
            if (!this.value || !this.value.trim()) return;

            if (this.id === "formNetworkIp") {
                validarLanTiempoReal(context);
            } else if (this.id === "formWanIp") {
                autocompletarWanVisual(context);
                validarWanTiempoReal(context);
            }
        });
    });
}

function configurarRefrescoFormularioCrear() {
    const createSection = document.getElementById("simpleCreateForm");
    const createForm = getCreateForm();

    if (createSection) {
        createSection.addEventListener("shown.bs.collapse", async function () {
            limpiarFormularioCrear(false);
            aplicarMascarasPorDefecto("create");
            await cargarSugerenciasIp("create");
        });

        createSection.addEventListener("hidden.bs.collapse", function () {
            limpiarFormularioCrear(true);
            aplicarMascarasPorDefecto("create");
        });
    }

    if (createForm) {
        createForm.addEventListener("reset", function () {
            setTimeout(async () => {
                limpiarFormularioCrear(false);
                aplicarMascarasPorDefecto("create");
                await cargarSugerenciasIp("create");
            }, 0);
        });
    }
}

function configurarDecisionTraslape(context) {
    const check = getAceptaTraslapeElement(context);
    const justificacion = getMotivoJustificacionElement(context);

    if (check) {
        check.addEventListener("change", function () {
            actualizarBloqueJustificacion(context);
            sincronizarResumenJustificacionModal();
            actualizarEstadoGuardar(context);
        });
    }

    if (justificacion) {
        justificacion.addEventListener("input", function () {
            validarJustificacion(context);
            sincronizarResumenJustificacionModal();
            actualizarEstadoGuardar(context);
        });
    }
}

function configurarGuardarYCompartirCorreo() {
    const btn = document.getElementById("btnGuardarYCompartirCorreo");
    const form = getCreateForm();

    if (!btn || !form) return;

    btn.addEventListener("click", function () {
        const hidden = ensurePostSaveActionInput(form);
        hidden.value = "email";
        form.requestSubmit();
    });

    form.addEventListener("submit", function (event) {
        const hidden = ensurePostSaveActionInput(form);

        if (event.submitter && event.submitter.id === "btnGuardarRegistro") {
            hidden.value = "";
        }
    });
}

function ensurePostSaveActionInput(form) {
    let input = form.querySelector('input[name="postSaveAction"]');

    if (!input) {
        input = document.createElement("input");
        input.type = "hidden";
        input.name = "postSaveAction";
        input.value = "";
        form.appendChild(input);
    }

    return input;
}

function abrirCorreoDireccionamientoDesdeFormulario() {
    const nombreLugar = (document.getElementById("createNombreLugar")?.value || "").trim();
    const lanIp = (document.getElementById("formNetworkIp")?.value || "").trim();
    const lanMask = (document.getElementById("formCidr")?.value || "").trim();
    const wanIp = (document.getElementById("formWanIp")?.value || "").trim();
    const wanMask = (document.getElementById("formWanCidr")?.value || "").trim();

    const wan1 = esIpValida(wanIp) ? sumarIp(wanIp, 1) : "";
    const wan2 = esIpValida(wanIp) ? sumarIp(wanIp, 2) : "";
    const broadcast = esIpValida(wanIp) ? sumarIp(wanIp, 3) : "";

    const paraDefault = "WRedondo@ice.go.cr";
    const ccDefault = "jlherrer@ccss.sa.cr; fbarboza@ccss.sa.cr; goabarca@ccss.sa.cr";

    const para = prompt("Para:", paraDefault);
    if (para === null) return;

    const cc = prompt("Copia a:", ccDefault);
    if (cc === null) return;

    const asunto = `Solicitud de direccionamiento a ${nombreLugar || ""}`.trim();
    const saludo = obtenerSaludoPorHora();

    const cuerpo = [
        saludo,
        "",
        "Envio lo solicitado:",
        "",
        `Lugar: ${nombreLugar || ""}`,
        `LAN: ${lanIp && lanMask ? lanIp + "/" + lanMask : ""}`,
        `WAN: ${wanIp && wanMask ? wanIp + "/" + wanMask : ""}`,
        `WAN+1: ${wan1 || ""}`,
        `WAN+2: ${wan2 || ""}`,
        `Broadcast: ${broadcast || ""}`,
        "",
        "Saludos,"
    ].join("\n");

    const mailto =
        `mailto:${encodeURIComponent(normalizarCorreos(para))}` +
        `?cc=${encodeURIComponent(normalizarCorreos(cc))}` +
        `&subject=${encodeURIComponent(asunto)}` +
        `&body=${encodeURIComponent(cuerpo)}`;

    window.location.href = mailto;
}

function procesarCorreoPostGuardado() {
    if (!window.emailPostSaveData || window.emailPostSaveData.autoOpen !== true) {
        return;
    }

    setTimeout(() => {
        abrirCorreoDireccionamientoDesdeDatos(window.emailPostSaveData);
        window.emailPostSaveData = null;
    }, 400);
}

function abrirCorreoDireccionamientoDesdeDatos(data) {
    if (!data) return;

    const paraDefault = "WRedondo@ice.go.cr";
    const ccDefault = "jlherrer@ccss.sa.cr; fbarboza@ccss.sa.cr; goabarca@ccss.sa.cr";

    const para = prompt("Para:", paraDefault);
    if (para === null) return;

    const cc = prompt("Copia a:", ccDefault);
    if (cc === null) return;

    const asunto = `Solicitud de direccionamiento a ${data.nombreLugar || ""}`.trim();
    const saludo = obtenerSaludoPorHora();

    const cuerpo = [
        saludo,
        "",
        "Envio lo solicitado:",
        "",
        `Lugar: ${data.nombreLugar || ""}`,
        `LAN: ${data.lan || ""}`,
        `WAN: ${data.wan || ""}`,
        `WAN+1: ${data.wan1 || ""}`,
        `WAN+2: ${data.wan2 || ""}`,
        `Broadcast: ${data.broadcast || ""}`,
        "",
        "Saludos,"
    ].join("\n");

    const mailto =
        `mailto:${encodeURIComponent(normalizarCorreos(para))}` +
        `?cc=${encodeURIComponent(normalizarCorreos(cc))}` +
        `&subject=${encodeURIComponent(asunto)}` +
        `&body=${encodeURIComponent(cuerpo)}`;

    window.location.href = mailto;
}

function obtenerSaludoPorHora() {
    const now = new Date();
    const hour = now.getHours();
    const minute = now.getMinutes();
    const totalMinutes = hour * 60 + minute;

    if (totalMinutes >= 0 && totalMinutes <= 719) {
        return "Buenos días";
    }
    if (totalMinutes >= 720 && totalMinutes <= 1079) {
        return "Buenas tardes";
    }
    return "Buenas noches";
}

function normalizarCorreos(texto) {
    if (!texto) return "";
    return texto
        .split(/[;,]+/)
        .map(x => x.trim())
        .filter(Boolean)
        .join(",");
}

function encodeMailList(texto) {
    return encodeURIComponent(normalizarCorreos(texto));
}

function getValue(context, id) {
    const el = getScopedElement(context, id);
    return el ? (el.value || "").trim() : "";
}

function getAceptaTraslapeElement(context) {
    return context === "create"
        ? document.getElementById("createAceptaTraslape")
        : getScopedElement("modal", "formAceptaTraslape");
}

function getMotivoJustificacionElement(context) {
    return context === "create"
        ? document.getElementById("createMotivoJustificacion")
        : getScopedElement("modal", "formMotivoJustificacion");
}

function getTraslapeDecisionBlock(context) {
    return context === "create"
        ? document.getElementById("createTraslapeDecisionBlock")
        : getScopedElement("modal", "traslapeDecisionBlock");
}

function getTraslapeDecisionLabel(context) {
    return context === "create"
        ? document.getElementById("createTraslapeDecisionLabel")
        : getScopedElement("modal", "traslapeDecisionLabel");
}

function getJustificacionBlock(context) {
    return context === "create"
        ? document.getElementById("createJustificacionBlock")
        : getScopedElement("modal", "justificacionBlock");
}

function getJustificacionFeedbackId(context) {
    return context === "create" ? "createJustificacionFeedback" : "justificacionFeedback";
}

function cerrarFormularioCrear() {
    const createSection = document.getElementById("simpleCreateForm");
    if (!createSection) return;

    const collapseInstance = bootstrap.Collapse.getOrCreateInstance(createSection, {
        toggle: false
    });

    collapseInstance.hide();
}

function aplicarMascarasPorDefecto(context) {
    const formCidr = getScopedElement(context, "formCidr");
    const formWanCidr = getScopedElement(context, "formWanCidr");

    if (formCidr && (!formCidr.value || formCidr.value.trim() === "")) {
        formCidr.value = "24";
    }

    if (formWanCidr && (!formWanCidr.value || formWanCidr.value.trim() === "")) {
        formWanCidr.value = "30";
    }
}

function limpiarFormularioCrear(limpiarPlaceholders = false) {
    const formId = getScopedElement("create", "formId");
    const formEnlace = getScopedElement("create", "formEnlace");
    const formNetworkIp = getScopedElement("create", "formNetworkIp");
    const formCidr = getScopedElement("create", "formCidr");
    const formWanIp = getScopedElement("create", "formWanIp");
    const formWanCidr = getScopedElement("create", "formWanCidr");
    const nombreLugar = getScopedElement("create", "createNombreLugar");

    if (formId) formId.value = "";
    if (formEnlace) formEnlace.value = "";
    if (formNetworkIp) formNetworkIp.value = "";
    if (formCidr) formCidr.value = "";
    if (formWanIp) formWanIp.value = "";
    if (formWanCidr) formWanCidr.value = "";
    if (nombreLugar) nombreLugar.value = "";

    if (limpiarPlaceholders) {
        if (formNetworkIp) formNetworkIp.placeholder = "";
        if (formWanIp) formWanIp.placeholder = "";
    }

    resetearBloqueTraslape("create");

    mostrarFeedbackContext("create", "enlaceFeedback", "", "");
    mostrarFeedbackContext("create", "lanFeedback", "", "");
    mostrarFeedbackContext("create", "wanFeedback", "", "");

    validationState.create.enlaceValido = true;
    validationState.create.lanValida = true;
    validationState.create.wanValida = true;
    validationState.create.lanTieneTraslape = false;
    validationState.create.wanTieneTraslape = false;
    actualizarEstadoGuardar("create");
}

function resetearBloqueTraslape(context) {
    const block = getTraslapeDecisionBlock(context);
    const label = getTraslapeDecisionLabel(context);
    const check = getAceptaTraslapeElement(context);
    const justificacionBlock = getJustificacionBlock(context);
    const justificacion = getMotivoJustificacionElement(context);

    if (block) block.classList.add("d-none");
    if (label) label.textContent = "Deseo continuar con el registro aunque exista traslape.";
    if (check) check.checked = false;
    if (justificacionBlock) justificacionBlock.classList.add("d-none");
    if (justificacion) justificacion.value = "";

    if (context === "modal") {
        const justificacionResumen = getScopedElement("modal", "formMotivoJustificacionResumen");
        const aceptadoPor = getScopedElement("modal", "formTraslapeAceptadoPor");
        const aceptadoAt = getScopedElement("modal", "formTraslapeAceptadoAt");
        const aceptadoAtTexto = getScopedElement("modal", "formTraslapeAceptadoAtTexto");

        if (justificacionResumen) justificacionResumen.value = "";
        if (aceptadoPor) aceptadoPor.value = "";
        if (aceptadoAt) aceptadoAt.value = "";
        if (aceptadoAtTexto) aceptadoAtTexto.value = "";
    }

    mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "", "");
}

function actualizarBloqueTraslape(context) {
    const state = validationState[context];
    const block = getTraslapeDecisionBlock(context);
    const label = getTraslapeDecisionLabel(context);
    const check = getAceptaTraslapeElement(context);
    const justificacionBlock = getJustificacionBlock(context);

    if (!block || !label || !check || !justificacionBlock) return;

    const hayTraslape = state.lanTieneTraslape || state.wanTieneTraslape;

    if (!hayTraslape) {
        block.classList.add("d-none");
        check.checked = false;
        justificacionBlock.classList.add("d-none");
        mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "", "");
        sincronizarResumenJustificacionModal();
        return;
    }

    block.classList.remove("d-none");

    if (state.lanTieneTraslape && state.wanTieneTraslape) {
        label.textContent = "La LAN y la WAN traslapan. ¿Desea continuar?";
    } else if (state.lanTieneTraslape) {
        label.textContent = "La LAN traslapa. ¿Desea continuar?";
    } else if (state.wanTieneTraslape) {
        label.textContent = "La WAN traslapa. ¿Desea continuar?";
    }

    actualizarBloqueJustificacion(context);
    sincronizarResumenJustificacionModal();
}

function actualizarBloqueJustificacion(context) {
    const check = getAceptaTraslapeElement(context);
    const justificacionBlock = getJustificacionBlock(context);

    if (!check || !justificacionBlock) return;

    if (check.checked) {
        justificacionBlock.classList.remove("d-none");
    } else {
        justificacionBlock.classList.add("d-none");
        mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "", "");
    }
}

function validarJustificacion(context) {
    const check = getAceptaTraslapeElement(context);
    const justificacion = getMotivoJustificacionElement(context);

    if (!check || !justificacion) return true;

    if (!check.checked) {
        mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "", "");
        return true;
    }

    if (!justificacion.value || !justificacion.value.trim()) {
        mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "warning", "Debe indicar una justificación para continuar.");
        return false;
    }

    mostrarFeedbackContext(context, getJustificacionFeedbackId(context), "success", "Justificación ingresada.");
    return true;
}

function sincronizarResumenJustificacionModal() {
    const motivo = getScopedElement("modal", "formMotivoJustificacion");
    const resumen = getScopedElement("modal", "formMotivoJustificacionResumen");
    if (motivo && resumen) {
        resumen.value = motivo.value || "";
    }
}

async function cargarSugerenciasIp(context) {
    await Promise.all([
        cargarSugerenciaIndividual(context, "lan"),
        cargarSugerenciaIndividual(context, "wan")
    ]);
}

async function cargarSugerenciaIndividual(context, tipo) {
    const input = tipo === "lan"
        ? getScopedElement(context, "formNetworkIp")
        : getScopedElement(context, "formWanIp");

    if (!input) return;

    try {
        const response = await getJson(`/api/validate/siguiente-ip?tipo=${tipo}`);
        if (!response.ok) return;

        const data = await response.json();
        if (!data || !data.ip) return;

        input.placeholder = data.ip;
    } catch (e) {
        console.error(`No fue posible cargar sugerencia ${tipo}:`, e);
    }
}

function limitarMascara(input) {
    if (!input) return;
    let value = input.value.replace(/\D/g, "");
    if (value === "") {
        input.value = "";
        return;
    }
    let num = parseInt(value, 10);
    if (num < 0) num = 0;
    if (num > 32) num = 32;
    input.value = num;
}

function visualizarRegistro(button) {
    abrirEditarModal(button);

    const form = getModalForm();
    const titulo = getScopedElement("modal", "registroModalLabel");
    const guardarBtn = getScopedElement("modal", "btnGuardarRegistro");
    const traslapeBlock = getScopedElement("modal", "traslapeDecisionBlock");

    if (titulo) titulo.innerText = "Visualizar registro";
    if (guardarBtn) guardarBtn.style.display = "none";

    if (form) {
        const elements = form.querySelectorAll("input, textarea, select, button");
        elements.forEach(el => {
            if (el.id === "btnGuardarRegistro") return;
            if (el.classList.contains("btn-close")) return;
            if (el.getAttribute("data-bs-dismiss") === "modal") return;
            if (el.type === "hidden") return;
            el.setAttribute("disabled", "disabled");
        });
    }

    if (traslapeBlock) {
        traslapeBlock.classList.add("d-none");
    }
}

function abrirEditarModal(button) {
    const row = button.closest("tr");
    if (!row) return;

    const label = getScopedElement("modal", "registroModalLabel");
    if (label) label.innerText = "Ver / Editar registro";

    const form = getModalForm();
    const guardarBtn = getScopedElement("modal", "btnGuardarRegistro");

    if (form) {
        const elements = form.querySelectorAll("input, textarea, select, button");
        elements.forEach(el => {
            if (el.id === "btnGuardarRegistro") return;
            if (el.classList.contains("btn-close")) return;
            if (el.getAttribute("data-bs-dismiss") === "modal") return;
            if (el.type === "hidden") return;
            el.removeAttribute("disabled");
        });
    }

    if (guardarBtn) {
        guardarBtn.style.display = "inline-block";
    }

    setModalValue("formId", row.dataset.realId || "");
    setModalValue("formEsHistorico", "false");
    setModalValue("formNombreLugar", row.dataset.nombre || "");
    setModalValue("formEnlace", row.dataset.enlace || "");
    setModalValue("formNetworkIp", row.dataset.networkip || "");
    setModalValue("formCidr", row.dataset.cidr || "");
    setModalValue("formWanIp", row.dataset.wanip || "");
    setModalValue("formWanCidr", row.dataset.wancidr || "");
    setModalValue("formWanIp1", row.dataset.wanip1 || "");
    setModalValue("formWanIp2", row.dataset.wanip2 || "");
    setModalValue("formWanBroadcast", row.dataset.wanbroadcast || "");
    setModalValue("modalConflicto", row.dataset.conflicto === "true" ? "Sí" : "No");
    setModalValue("modalDetalle", row.dataset.detalle || "");
    setModalValue("formMotivoJustificacion", row.dataset.motivojustificacion || "");
    setModalValue("formMotivoJustificacionResumen", row.dataset.motivojustificacion || "");
    setModalValue("formTraslapeAceptadoPor", row.dataset.traslapeaceptadopor || "");
    setModalValue("formTraslapeAceptadoAt", row.dataset.traslapeaceptadoat || "");

    const aceptadoAtTexto = getScopedElement("modal", "formTraslapeAceptadoAtTexto");
    if (aceptadoAtTexto) {
        aceptadoAtTexto.value = row.dataset.traslapeaceptadoattexto || "";
    }

    const check = getAceptaTraslapeElement("modal");
    if (check) {
        check.checked = row.dataset.aceptatraslape === "true";
    }

    const lanExplanation = row.querySelector(".lan-explanation-source")?.value || "";
    const wanExplanation = row.querySelector(".wan-explanation-source")?.value || "";

    setConflictPanelState("modal", "lan", row.dataset.tienelanconflicto === "true", lanExplanation);
    setConflictPanelState("modal", "wan", row.dataset.tienewanconflicto === "true", wanExplanation);

    validationState.modal.lanTieneTraslape = row.dataset.tienelanconflicto === "true";
    validationState.modal.wanTieneTraslape = row.dataset.tienewanconflicto === "true";
    validationState.modal.enlaceValido = true;
    validationState.modal.lanValida = true;
    validationState.modal.wanValida = true;

    actualizarBloqueTraslape("modal");
    validarJustificacion("modal");
    sincronizarResumenJustificacionModal();

    mostrarFeedbackContext("modal", "enlaceFeedback", "", "");
    mostrarFeedbackContext("modal", "lanFeedback", "", "");
    mostrarFeedbackContext("modal", "wanFeedback", "", "");

    actualizarEstadoGuardar("modal");

    const modalElement = getModalElement();
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
}

function setModalValue(id, value) {
    const el = getScopedElement("modal", id);
    if (el) el.value = value || "";
}

function confirmarEliminar(button) {
    const row = button.closest("tr");
    if (!row) return;

    const realId = row.dataset.realId;
    if (confirm("¿Está seguro de que desea eliminar este registro?")) {
        const deleteId = document.getElementById("deleteId");
        const deleteForm = document.getElementById("deleteForm");
        if (deleteId) deleteId.value = realId;
        if (deleteForm) deleteForm.submit();
    }
}

function toggleSortByColumn(colIndex, type = "text") {
    const table = document.getElementById("networksTable");
    if (!table) return;

    const tbody = table.querySelector("tbody");
    const rows = Array.from(tbody.querySelectorAll("tr"));

    const currentDir = sortStates[colIndex] === "asc" ? "asc" : "desc";
    const newDir = currentDir === "asc" ? "desc" : "asc";
    sortStates[colIndex] = newDir;

    rows.sort((a, b) => {
        let aVal = (a.cells[colIndex]?.innerText || "").trim();
        let bVal = (b.cells[colIndex]?.innerText || "").trim();

        if (type === "number") {
            const numA = parseFloat(aVal) || 0;
            const numB = parseFloat(bVal) || 0;
            return newDir === "asc" ? numA - numB : numB - numA;
        }

        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();

        return newDir === "asc"
            ? aVal.localeCompare(bVal, "es", { numeric: true })
            : bVal.localeCompare(aVal, "es", { numeric: true });
    });

    rows.forEach(row => tbody.appendChild(row));

    document.querySelectorAll(".sort-indicator").forEach(ind => ind.innerText = "↕");
    const activeIndicator = document.querySelector(`.sort-indicator[data-col="${colIndex}"]`);
    if (activeIndicator) {
        activeIndicator.innerText = newDir === "asc" ? "↑" : "↓";
    }

    currentPage = 1;
    aplicarFiltrosTabla();
}

function toggleSidebar() {
    document.body.classList.toggle("sidebar-collapsed");
}

function toggleConflictPanel(type) {
    const panel = getScopedElement("modal", `${type}ConflictPanel`);
    if (!panel) return;
    panel.classList.toggle("d-none");
}

function setConflictPanelState(context, type, hasConflict, explanation) {
    const btn = getScopedElement(context, `${type}InfoBtn`);
    const panel = getScopedElement(context, `${type}ConflictPanel`);
    const text = getScopedElement(context, `${type}ConflictExplanationText`);

    if (!btn || !panel || !text) return;

    text.textContent = explanation || "";

    if (hasConflict && explanation && explanation.trim() !== "") {
        btn.classList.remove("d-none");
    } else {
        btn.classList.add("d-none");
        panel.classList.add("d-none");
        text.textContent = "";
    }
}

async function validarEnlaceTiempoReal(context) {
    const formEnlace = getScopedElement(context, "formEnlace");
    const formId = getScopedElement(context, "formId");
    if (!formEnlace) return;

    const enlace = formEnlace.value.trim();
    const id = formId ? formId.value : "";

    if (!enlace) {
        mostrarFeedbackContext(context, "enlaceFeedback", "", "");
        validationState[context].enlaceValido = true;
        actualizarEstadoGuardar(context);
        return;
    }

    if (enlace.length < 8) {
        mostrarFeedbackContext(context, "enlaceFeedback", "warning", "El enlace debe tener 8 dígitos.");
        validationState[context].enlaceValido = false;
        actualizarEstadoGuardar(context);
        return;
    }

    const res = await postJson("/api/validate/enlace", { enlace, id });
    const data = await res.json();

    validationState[context].enlaceValido = !!data.ok;
    mostrarFeedbackContext(context, "enlaceFeedback", data.ok ? "success" : "error", data.message || "");
    actualizarEstadoGuardar(context);
}

async function validarLanTiempoReal(context) {
    const formNetworkIp = getScopedElement(context, "formNetworkIp");
    const formCidr = getScopedElement(context, "formCidr");
    const formId = getScopedElement(context, "formId");
    if (!formNetworkIp || !formCidr) return;

    const ip = formNetworkIp.value.trim();
    const cidrValue = formCidr.value.trim();
    const id = formId ? formId.value : "";

    if (!ip || !cidrValue) {
        mostrarFeedbackContext(context, "lanFeedback", "", "");
        validationState[context].lanValida = true;
        validationState[context].lanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    if (ip === "0.0.0.0") {
        mostrarFeedbackContext(context, "lanFeedback", "", "");
        validationState[context].lanValida = true;
        validationState[context].lanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    if (!esIpCompleta(ip)) {
        const mensajeParcial = buscarCoincidenciaParcial(ip, "lan");
        if (mensajeParcial) {
            mostrarFeedbackContext(context, "lanFeedback", "warning", mensajeParcial);
        } else {
            mostrarFeedbackContext(context, "lanFeedback", "", "");
        }
        validationState[context].lanValida = true;
        validationState[context].lanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    const res = await postJson("/api/validate/lan", {
        ip,
        cidr: cidrValue,
        id
    });
    const data = await res.json();

    validationState[context].lanValida = true;
    validationState[context].lanTieneTraslape = !data.ok;

    mostrarFeedbackContext(context, "lanFeedback", data.ok ? "success" : "error", data.message || "");
    actualizarBloqueTraslape(context);
    actualizarEstadoGuardar(context);
}

async function validarWanTiempoReal(context) {
    const formWanIp = getScopedElement(context, "formWanIp");
    const formWanCidr = getScopedElement(context, "formWanCidr");
    const formId = getScopedElement(context, "formId");
    if (!formWanIp || !formWanCidr) return;

    const ip = formWanIp.value.trim();
    const cidrValue = formWanCidr.value.trim();
    const id = formId ? formId.value : "";

    if (!ip || !cidrValue) {
        mostrarFeedbackContext(context, "wanFeedback", "", "");
        validationState[context].wanValida = true;
        validationState[context].wanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    if (ip === "0.0.0.0") {
        mostrarFeedbackContext(context, "wanFeedback", "", "");
        validationState[context].wanValida = true;
        validationState[context].wanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    if (!esIpCompleta(ip)) {
        const mensajeParcial = buscarCoincidenciaParcial(ip, "wan");
        if (mensajeParcial) {
            mostrarFeedbackContext(context, "wanFeedback", "warning", mensajeParcial);
        } else {
            mostrarFeedbackContext(context, "wanFeedback", "", "");
        }
        validationState[context].wanValida = true;
        validationState[context].wanTieneTraslape = false;
        actualizarBloqueTraslape(context);
        actualizarEstadoGuardar(context);
        return;
    }

    const res = await postJson("/api/validate/wan", {
        ip,
        cidr: cidrValue,
        id
    });
    const data = await res.json();

    validationState[context].wanValida = true;
    validationState[context].wanTieneTraslape = !data.ok;

    mostrarFeedbackContext(context, "wanFeedback", data.ok ? "success" : "error", data.message || "");
    actualizarBloqueTraslape(context);
    actualizarEstadoGuardar(context);
}

function buscarCoincidenciaParcial(ipParcial, tipo) {
    const normalizado = limpiarIpParcial(ipParcial);
    if (!normalizado) return "";

    const octetos = normalizado.split(".").filter(Boolean);
    if (octetos.length < 2) return "";

    const prefix = octetos.join(".");
    const existentes = obtenerRedesExistentes(tipo);

    for (const red of existentes) {
        if (red.ip === "0.0.0.0") continue;
        if (red.ip.startsWith(prefix + ".") || red.ip === prefix) {
            return `Posible traslape con ${tipo.toUpperCase()} existente: ${red.ip}/${red.cidr}`;
        }
    }

    return "";
}

function obtenerRedesExistentes(tipo) {
    const table = document.getElementById("networksTable");
    if (!table) return [];

    const rows = table.querySelectorAll("tbody tr");
    const colIndex = tipo === "lan" ? 3 : 4;
    const redes = [];

    rows.forEach(row => {
        const text = (row.cells[colIndex]?.innerText || "").trim();
        const data = separarIpCidr(text);
        if (data) {
            redes.push(data);
        }
    });

    return redes;
}

function separarIpCidr(texto) {
    if (!texto || !texto.includes("/")) return null;
    const partes = texto.split("/");
    if (partes.length !== 2) return null;

    const ip = partes[0].trim();
    const cidr = parseInt(partes[1].trim(), 10);

    if (!ip || isNaN(cidr)) return null;

    return { ip, cidr };
}

function limpiarIpParcial(ip) {
    if (!ip) return "";
    return ip.replace(/[^0-9.]/g, "").replace(/\.{2,}/g, ".").replace(/^\./, "").replace(/\.$/, "");
}

function autocompletarWanVisual(context) {
    const wanIpInput = getScopedElement(context, "formWanIp");
    const wanIp1Input = getScopedElement(context, "formWanIp1");
    const wanIp2Input = getScopedElement(context, "formWanIp2");
    const wanBroadcastInput = getScopedElement(context, "formWanBroadcast");

    if (!wanIpInput) return;

    const wanIp = wanIpInput.value.trim();

    if (!wanIp || !esIpValida(wanIp)) {
        if (wanIp1Input) wanIp1Input.value = "";
        if (wanIp2Input) wanIp2Input.value = "";
        if (wanBroadcastInput) wanBroadcastInput.value = "";
        return;
    }

    if (wanIp1Input) wanIp1Input.value = sumarIp(wanIp, 1);
    if (wanIp2Input) wanIp2Input.value = sumarIp(wanIp, 2);
    if (wanBroadcastInput) wanBroadcastInput.value = sumarIp(wanIp, 3);
}

function esIpValida(ip) {
    const partes = ip.split(".");
    if (partes.length !== 4) return false;

    for (const p of partes) {
        const n = Number(p);
        if (!Number.isInteger(n) || n < 0 || n > 255) return false;
    }
    return true;
}

function esIpCompleta(text) {
    return esIpValida(text);
}

function ipToLong(ip) {
    const p = ip.split(".").map(Number);
    return (((p[0] * 256 + p[1]) * 256 + p[2]) * 256 + p[3]) >>> 0;
}

function longToIp(value) {
    return [
        (value >>> 24) & 255,
        (value >>> 16) & 255,
        (value >>> 8) & 255,
        value & 255
    ].join(".");
}

function sumarIp(ip, add) {
    return longToIp((ipToLong(ip) + add) >>> 0);
}

function networkAddress(ip, cidr) {
    const ipLong = ipToLong(ip);
    const mask = cidr === 0 ? 0 : ((0xFFFFFFFF << (32 - cidr)) >>> 0);
    return (ipLong & mask) >>> 0;
}

function broadcastAddress(network, cidr) {
    const mask = cidr === 0 ? 0 : ((0xFFFFFFFF << (32 - cidr)) >>> 0);
    const wildcard = (~mask) >>> 0;
    return (network | wildcard) >>> 0;
}

function mostrarFeedbackContext(context, elementId, type, message) {
    const el = getScopedElement(context, elementId) || document.getElementById(elementId);
    if (!el) return;

    el.className = "live-feedback";
    el.textContent = message || "";

    if (type === "success") {
        el.classList.add("feedback-success");
    } else if (type === "error") {
        el.classList.add("feedback-error");
    } else if (type === "warning") {
        el.classList.add("feedback-warning");
    }
}

function actualizarEstadoGuardar(context) {
    const btn = getScopedElement(context, "btnGuardarRegistro");
    if (!btn) return;

    const state = validationState[context];
    const hayTraslape = state.lanTieneTraslape || state.wanTieneTraslape;
    const check = getAceptaTraslapeElement(context);
    const justificacionValida = validarJustificacionSilenciosa(context);

    if (hayTraslape) {
        btn.disabled = !(state.enlaceValido && state.lanValida && state.wanValida && check && check.checked && justificacionValida);
        return;
    }

    btn.disabled = !(state.enlaceValido && state.lanValida && state.wanValida);
}

function validarJustificacionSilenciosa(context) {
    const check = getAceptaTraslapeElement(context);
    const justificacion = getMotivoJustificacionElement(context);

    if (!check || !justificacion) return true;
    if (!check.checked) return true;

    return !!(justificacion.value && justificacion.value.trim());
}

function configurarSesionInactiva() {
    const modalElement = document.getElementById("sessionWarningModal");
    if (!modalElement) return;

    sessionWarningModalInstance = new bootstrap.Modal(modalElement, {
        backdrop: "static",
        keyboard: false
    });

    reiniciarTemporizadoresSesion();

    ["click", "mousemove", "keydown", "scroll"].forEach(evt => {
        document.addEventListener(evt, () => reiniciarTemporizadoresSesion(), true);
    });
}

function reiniciarTemporizadoresSesion() {
    clearTimeout(sessionWarningTimeout);
    clearTimeout(sessionLogoutTimeout);

    sessionWarningTimeout = setTimeout(() => {
        if (sessionWarningModalInstance) {
            sessionWarningModalInstance.show();
        }
    }, 6 * 60 * 1000);

    sessionLogoutTimeout = setTimeout(() => {
        logoutNow();
    }, 7 * 60 * 1000);
}

async function extendSession() {
    try {
        await postJson("/session/ping", {});
    } catch (e) {
        logoutNow();
        return;
    }

    if (sessionWarningModalInstance) {
        sessionWarningModalInstance.hide();
    }
    reiniciarTemporizadoresSesion();
}

function logoutNow() {
    const form = document.createElement("form");
    form.method = "POST";
    form.action = "/logout";

    const input = document.createElement("input");
    input.type = "hidden";
    input.name = "_csrf";
    input.value = getCsrfToken();

    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
}

function abrirCalculadoraIP() {
    const calc = document.getElementById("ipCalcWindow");
    if (calc) {
        calc.style.display = "block";
    }
}

function cerrarCalculadoraIP() {
    const calc = document.getElementById("ipCalcWindow");
    if (calc) {
        calc.style.display = "none";
    }
}

function calcularIP() {
    const ipInput = document.getElementById("calcIp");
    const cidrInput = document.getElementById("calcCidr");
    const targetCidrInput = document.getElementById("calcTargetCidr");
    const resultado = document.getElementById("calcResultado");

    if (!ipInput || !cidrInput || !targetCidrInput || !resultado) return;

    const ip = ipInput.value.trim();
    const cidr = parseInt(cidrInput.value, 10);
    const targetCidr = parseInt(targetCidrInput.value, 10);

    if (!esIpValida(ip) || isNaN(cidr) || cidr < 0 || cidr > 32) {
        resultado.innerHTML = `
            <div style="
                background:#3a1f28;
                color:#ffd6d6;
                border:1px solid #a94442;
                border-radius:10px;
                padding:12px;
                font-size:15px;
                font-weight:600;
            ">
                IP o CIDR inválido
            </div>
        `;
        return;
    }

    const ipLong = ipToLong(ip);
    const networkLong = networkAddress(ip, cidr);
    const broadcastLong = broadcastAddress(networkLong, cidr);
    const network = longToIp(networkLong);
    const broadcast = longToIp(broadcastLong);

    const firstHost = cidr >= 31 ? network : longToIp(networkLong + 1);
    const lastHost = cidr >= 31 ? broadcast : longToIp(broadcastLong - 1);

    const totalIps = Math.pow(2, 32 - cidr);
    const usableHosts = cidr === 32 ? 1 : (cidr === 31 ? 2 : Math.max(totalIps - 2, 0));

    const clase = obtenerClaseIP(ip);
    const claseBaseCidr = obtenerClaseBaseCidr(clase);
    const mascaraDecimal = cidrToMask(cidr);
    const wildcard = longToIp((~ipToLong(mascaraDecimal)) >>> 0);
    const ipHex = ipToHex(ip);

    const ipBin = colorearBinarioPorSegmento(ip, claseBaseCidr, cidr);
    const networkBin = colorearBinarioPorSegmento(network, claseBaseCidr, cidr);
    const broadcastBin = colorearBinarioPorSegmento(broadcast, claseBaseCidr, cidr);

    let subneteoHtml = "";
    if (!isNaN(targetCidr) && targetCidr >= cidr && targetCidr <= 32) {
        const bitsPrestados = targetCidr - cidr;
        const cantidadSubredes = Math.pow(2, bitsPrestados);
        const ipsPorSubred = Math.pow(2, 32 - targetCidr);
        const hostsPorSubred = targetCidr === 32 ? 1 : (targetCidr === 31 ? 2 : Math.max(ipsPorSubred - 2, 0));

        subneteoHtml = `
            <div style="${calcCardStyle()}">
                <div style="${calcLabelStyle()}">Resumen de subneteo</div>
                <div style="${calcValueStyle()}">CIDR objetivo: /${targetCidr}</div>
                <div style="font-size:15px;color:#ffffff;line-height:1.5;">
                    Bits prestados: <strong>${bitsPrestados}</strong><br>
                    Subredes: <strong>${formatearNumero(cantidadSubredes)}</strong><br>
                    IPs por subred: <strong>${formatearNumero(ipsPorSubred)}</strong><br>
                    Hosts por subred: <strong>${formatearNumero(hostsPorSubred)}</strong>
                </div>
            </div>
        `;
    } else {
        subneteoHtml = `
            <div style="${calcCardStyle()}">
                <div style="${calcLabelStyle()}">Resumen de subneteo</div>
                <div style="font-size:15px;color:#ffd27a;">
                    El CIDR objetivo debe ser mayor o igual al CIDR actual.
                </div>
            </div>
        `;
    }

    resultado.innerHTML = `
        <div style="display:flex; flex-direction:column; gap:12px;">

            <div style="
                background:#2a2f4a;
                border:1px solid #3f4b7a;
                border-radius:12px;
                padding:14px;
                text-align:center;
                font-size:24px;
                font-weight:700;
                color:#ffffff;
            ">
                ${ip}/${cidr}
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Clase</div>
                    <div style="${calcValueStyle()}">${clase}</div>
                </div>
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Máscara decimal</div>
                    <div style="${calcValueStyle()}">${mascaraDecimal}</div>
                </div>

                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Wildcard</div>
                    <div style="${calcValueStyle()}">${wildcard}</div>
                </div>
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Red</div>
                    <div style="${calcValueStyle()}">${network}/${cidr}</div>
                </div>

                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Primer host</div>
                    <div style="${calcValueStyle()}">${firstHost}</div>
                </div>
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Último host</div>
                    <div style="${calcValueStyle()}">${lastHost}</div>
                </div>

                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Broadcast</div>
                    <div style="${calcValueStyle()}">${broadcast}</div>
                </div>
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">IPs totales</div>
                    <div style="${calcValueStyle()}">${formatearNumero(totalIps)}</div>
                </div>

                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Hosts utilizables</div>
                    <div style="${calcValueStyle()}">${formatearNumero(usableHosts)}</div>
                </div>
                <div style="${calcCardStyle()}">
                    <div style="${calcLabelStyle()}">Hexadecimal</div>
                    <div style="${calcValueStyle()}">${ipHex}</div>
                </div>
            </div>

            <div style="${calcCardStyle()}">
                <div style="${calcLabelStyle()}">Binario IP</div>
                <div style="${calcMonoStyle()}">${ipBin}</div>
            </div>

            <div style="${calcCardStyle()}">
                <div style="${calcLabelStyle()}">Binario Red</div>
                <div style="${calcMonoStyle()}">${networkBin}</div>
            </div>

            <div style="${calcCardStyle()}">
                <div style="${calcLabelStyle()}">Binario Broadcast</div>
                <div style="${calcMonoStyle()}">${broadcastBin}</div>
            </div>

            ${subneteoHtml}

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
                <button type="button" class="btn btn-outline-info" onclick="mostrarExplicacionCalculadora('${ip}', ${cidr})">
                    Explicación detallada
                </button>
                <button type="button" class="btn btn-outline-warning" onclick="listarSubredesCalculadora('${ip}', ${cidr}, ${targetCidr})">
                    Listar subredes
                </button>
                <button type="button" class="btn btn-outline-success" onclick="usarEnFormulario()">
                    Usar en LAN
                </button>
                <button type="button" class="btn btn-outline-primary" onclick="usarEnFormularioWAN()">
                    Usar en WAN
                </button>
            </div>
        </div>
    `;
}

function calcCardStyle() {
    return `
        background:#252844;
        border:1px solid #3e456d;
        border-radius:12px;
        padding:12px;
        min-height:84px;
        display:flex;
        flex-direction:column;
        justify-content:center;
        gap:6px;
    `;
}

function calcLabelStyle() {
    return `
        font-size:13px;
        font-weight:700;
        color:#9fb3ff;
        text-transform:uppercase;
        letter-spacing:.4px;
    `;
}

function calcValueStyle() {
    return `
        font-size:18px;
        font-weight:700;
        color:#ffffff;
        word-break:break-word;
        line-height:1.35;
    `;
}

function calcMonoStyle() {
    return `
        font-size:15px;
        font-weight:700;
        color:#ffffff;
        word-break:break-word;
        font-family:Consolas, 'Courier New', monospace;
        line-height:1.5;
    `;
}

function usarEnFormulario() {
    const ipInput = document.getElementById("calcIp");
    const cidrInput = document.getElementById("calcCidr");
    const formNetworkIp = getScopedElement("create", "formNetworkIp");
    const formCidr = getScopedElement("create", "formCidr");

    if (!ipInput || !cidrInput || !formNetworkIp || !formCidr) return;

    formNetworkIp.value = ipInput.value.trim();
    formCidr.value = cidrInput.value.trim();

    validarLanTiempoReal("create");
}

function usarEnFormularioWAN() {
    const ipInput = document.getElementById("calcIp");
    const cidrInput = document.getElementById("calcCidr");
    const formWanIp = getScopedElement("create", "formWanIp");
    const formWanCidr = getScopedElement("create", "formWanCidr");

    if (!ipInput || !cidrInput || !formWanIp || !formWanCidr) return;

    formWanIp.value = ipInput.value.trim();
    formWanCidr.value = cidrInput.value.trim();

    autocompletarWanVisual("create");
    validarWanTiempoReal("create");
}

function obtenerClaseIP(ip) {
    const primerOcteto = parseInt(ip.split(".")[0], 10);

    if (primerOcteto >= 1 && primerOcteto <= 126) return "A";
    if (primerOcteto >= 128 && primerOcteto <= 191) return "B";
    if (primerOcteto >= 192 && primerOcteto <= 223) return "C";
    if (primerOcteto >= 224 && primerOcteto <= 239) return "D";
    return "E";
}

function cidrToMask(cidr) {
    const maskLong = cidr === 0 ? 0 : ((0xFFFFFFFF << (32 - cidr)) >>> 0);
    return longToIp(maskLong);
}

function ipToHex(ip) {
    return ip.split(".")
        .map(oct => Number(oct).toString(16).toUpperCase().padStart(2, "0"))
        .join(".");
}

function ipToBinary(ip) {
    return ip.split(".")
        .map(oct => Number(oct).toString(2).padStart(8, "0"))
        .join(".");
}

function formatearNumero(numero) {
    return Number(numero).toLocaleString("es-CR");
}

function mostrarExplicacionCalculadora(ip, cidr) {
    const networkLong = networkAddress(ip, cidr);
    const broadcastLong = broadcastAddress(networkLong, cidr);
    const network = longToIp(networkLong);
    const broadcast = longToIp(broadcastLong);
    const totalIps = Math.pow(2, 32 - cidr);
    const usableHosts = cidr === 32 ? 1 : (cidr === 31 ? 2 : Math.max(totalIps - 2, 0));

    abrirModalCalculadora(
        "Explicación detallada",
        `
        <div style="display:grid; gap:12px; font-size:16px; line-height:1.7;">
            <div><strong>IP evaluada:</strong> ${ip}/${cidr}</div>
            <div><strong>Red calculada:</strong> ${network}/${cidr}</div>
            <div><strong>Broadcast:</strong> ${broadcast}</div>
            <div><strong>Total de IPs:</strong> ${formatearNumero(totalIps)}</div>
            <div><strong>Hosts utilizables:</strong> ${formatearNumero(usableHosts)}</div>
            <div><strong>Máscara decimal:</strong> ${cidrToMask(cidr)}</div>
            <div><strong>Explicación:</strong> el CIDR /${cidr} define cuántos bits pertenecen a la red. Los bits restantes quedan disponibles para hosts o para subneteo adicional.</div>
        </div>
        `
    );
}

function listarSubredesCalculadora(ip, cidr, targetCidr) {
    if (isNaN(targetCidr) || targetCidr < cidr || targetCidr > 32) {
        abrirModalCalculadora(
            "Listar subredes",
            `<div style="font-size:16px; color:#b00020;">El CIDR objetivo debe ser mayor o igual al CIDR actual.</div>`
        );
        return;
    }

    const baseNetwork = networkAddress(ip, cidr);
    const ipsPorSubred = Math.pow(2, 32 - targetCidr);
    const cantidadSubredes = Math.pow(2, targetCidr - cidr);
    const maxMostrar = Math.min(cantidadSubredes, 128);

    let html = `
        <div style="display:grid; gap:10px; font-size:15px;">
            <div><strong>Red base:</strong> ${longToIp(baseNetwork)}/${cidr}</div>
            <div><strong>CIDR objetivo:</strong> /${targetCidr}</div>
            <div><strong>Subredes generadas:</strong> ${formatearNumero(cantidadSubredes)}</div>
            <div><strong>IPs por subred:</strong> ${formatearNumero(ipsPorSubred)}</div>
            <hr>
    `;

    for (let i = 0; i < maxMostrar; i++) {
        const subredLong = baseNetwork + (i * ipsPorSubred);
        const subred = longToIp(subredLong);
        const primerHost = targetCidr >= 31 ? subred : longToIp(subredLong + 1);
        const ultimoHost = targetCidr >= 31 ? longToIp(subredLong + ipsPorSubred - 1) : longToIp(subredLong + ipsPorSubred - 2);
        const broadcast = longToIp(subredLong + ipsPorSubred - 1);

        html += `
            <div style="border:1px solid #ddd; border-radius:10px; padding:10px;">
                <div><strong>${i + 1}. ${subred}/${targetCidr}</strong></div>
                <div>Primer host: ${primerHost}</div>
                <div>Último host: ${ultimoHost}</div>
                <div>Broadcast: ${broadcast}</div>
            </div>
        `;
    }

    if (cantidadSubredes > maxMostrar) {
        html += `<div><strong>... y ${formatearNumero(cantidadSubredes - maxMostrar)} subredes más.</strong></div>`;
    }

    html += `</div>`;

    abrirModalCalculadora("Lista de subredes", html);
}

function abrirModalCalculadora(titulo, contenidoHtml) {
    const title = document.getElementById("calcInfoModalTitle");
    const body = document.getElementById("calcInfoModalBody");
    const modalEl = document.getElementById("calcInfoModal");

    if (!title || !body || !modalEl) return;

    title.innerHTML = titulo;
    body.innerHTML = contenidoHtml;

    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

function obtenerClaseBaseCidr(clase) {
    if (clase === "A") return 8;
    if (clase === "B") return 16;
    if (clase === "C") return 24;
    return 0;
}

function colorearBinarioPorSegmento(ip, claseBaseCidr, cidrActual) {
    const bits = ip.split(".")
        .map(oct => Number(oct).toString(2).padStart(8, "0"))
        .join("");

    let html = "";

    for (let i = 0; i < bits.length; i++) {
        let color = "#9be7ff";

        if (i < claseBaseCidr) {
            color = "#ff6b6b";
        } else if (i < cidrActual) {
            color = "#ffd166";
        } else {
            color = "#80ed99";
        }

        html += `<span style="color:${color};">${bits[i]}</span>`;

        if ((i + 1) % 8 === 0 && i < 31) {
            html += ".";
        }
    }

    html += `
        <div style="margin-top:8px; font-size:13px; display:flex; gap:14px; flex-wrap:wrap;">
            <span style="color:#ff6b6b;"><strong>■ Red base</strong></span>
            <span style="color:#ffd166;"><strong>■ Subred</strong></span>
            <span style="color:#80ed99;"><strong>■ Host</strong></span>
        </div>
    `;

    return html;
}

function configurarCalculadoraArrastrable() {
    const ventana = document.getElementById("ipCalcWindow");
    const header = document.getElementById("ipCalcHeader");

    if (!ventana || !header) return;

    let dragging = false;
    let offsetX = 0;
    let offsetY = 0;

    header.addEventListener("mousedown", function (e) {
        dragging = true;
        const rect = ventana.getBoundingClientRect();
        offsetX = e.clientX - rect.left;
        offsetY = e.clientY - rect.top;

        ventana.style.right = "auto";
    });

    document.addEventListener("mousemove", function (e) {
        if (!dragging) return;

        ventana.style.left = `${e.clientX - offsetX}px`;
        ventana.style.top = `${e.clientY - offsetY}px`;
    });

    document.addEventListener("mouseup", function () {
        dragging = false;
    });
}

function configurarBotonCerrarFormulario() {
    const createSection = document.getElementById("simpleCreateForm");
    const btnCerrar = document.getElementById("btnCerrarFormularioCrear");
    const btnToggle = document.getElementById("btnToggleCreateForm");
    const icon = document.getElementById("createToggleIcon");

    if (!createSection || !btnCerrar || !btnToggle || !icon) return;

    btnCerrar.style.display = "none";
    icon.textContent = "+";

    createSection.addEventListener("shown.bs.collapse", function () {
        btnCerrar.style.display = "inline-block";
        icon.textContent = "−";
        btnToggle.setAttribute("aria-expanded", "true");
    });

    createSection.addEventListener("hidden.bs.collapse", function () {
        btnCerrar.style.display = "none";
        icon.textContent = "+";
        btnToggle.setAttribute("aria-expanded", "false");
    });
}

function visualizarRegistro(button) {
    abrirEditarModal(button);

    const form = getModalForm();
    const titulo = getScopedElement("modal", "registroModalLabel");
    const guardarBtn = getScopedElement("modal", "btnGuardarRegistro");
    const traslapeBlock = getScopedElement("modal", "traslapeDecisionBlock");

    if (titulo) titulo.innerText = "Visualizar registro";
    if (guardarBtn) guardarBtn.style.display = "none";

    if (form) {
        const elements = form.querySelectorAll("input, textarea, select, button");
        elements.forEach(el => {
            if (el.id === "btnGuardarRegistro") return;
            if (el.classList.contains("btn-close")) return;
            if (el.getAttribute("data-bs-dismiss") === "modal") return;
            if (el.type === "hidden") return;
            el.setAttribute("disabled", "disabled");
        });
    }

    if (traslapeBlock) {
        traslapeBlock.classList.add("d-none");
    }
}

function abrirEditarModal(button) {
    const row = button.closest("tr");
    if (!row) return;

    const label = getScopedElement("modal", "registroModalLabel");
    if (label) label.innerText = "Ver / Editar registro";

    const form = getModalForm();
    const guardarBtn = getScopedElement("modal", "btnGuardarRegistro");

    if (form) {
        const elements = form.querySelectorAll("input, textarea, select, button");
        elements.forEach(el => {
            if (el.id === "btnGuardarRegistro") return;
            if (el.classList.contains("btn-close")) return;
            if (el.getAttribute("data-bs-dismiss") === "modal") return;
            if (el.type === "hidden") return;
            el.removeAttribute("disabled");
        });
    }

    if (guardarBtn) {
        guardarBtn.style.display = "inline-block";
    }

    setModalValue("formId", row.dataset.realId || "");
    setModalValue("formEsHistorico", "false");
    setModalValue("formNombreLugar", row.dataset.nombre || "");
    setModalValue("formEnlace", row.dataset.enlace || "");
    setModalValue("formNetworkIp", row.dataset.networkip || "");
    setModalValue("formCidr", row.dataset.cidr || "");
    setModalValue("formWanIp", row.dataset.wanip || "");
    setModalValue("formWanCidr", row.dataset.wancidr || "");
    setModalValue("formWanIp1", row.dataset.wanip1 || "");
    setModalValue("formWanIp2", row.dataset.wanip2 || "");
    setModalValue("formWanBroadcast", row.dataset.wanbroadcast || "");
    setModalValue("modalConflicto", row.dataset.conflicto === "true" ? "Sí" : "No");
    setModalValue("modalDetalle", row.dataset.detalle || "");
    setModalValue("formMotivoJustificacion", row.dataset.motivojustificacion || "");
    setModalValue("formMotivoJustificacionResumen", row.dataset.motivojustificacion || "");
    setModalValue("formTraslapeAceptadoPor", row.dataset.traslapeaceptadopor || "");
    setModalValue("formTraslapeAceptadoAt", row.dataset.traslapeaceptadoat || "");

    const aceptadoAtTexto = getScopedElement("modal", "formTraslapeAceptadoAtTexto");
    if (aceptadoAtTexto) {
        aceptadoAtTexto.value = row.dataset.traslapeaceptadoattexto || "";
    }

    const check = getAceptaTraslapeElement("modal");
    if (check) {
        check.checked = row.dataset.aceptatraslape === "true";
    }

    const lanExplanation = row.querySelector(".lan-explanation-source")?.value || "";
    const wanExplanation = row.querySelector(".wan-explanation-source")?.value || "";

    setConflictPanelState("modal", "lan", row.dataset.tienelanconflicto === "true", lanExplanation);
    setConflictPanelState("modal", "wan", row.dataset.tienewanconflicto === "true", wanExplanation);

    validationState.modal.lanTieneTraslape = row.dataset.tienelanconflicto === "true";
    validationState.modal.wanTieneTraslape = row.dataset.tienewanconflicto === "true";
    validationState.modal.enlaceValido = true;
    validationState.modal.lanValida = true;
    validationState.modal.wanValida = true;

    actualizarBloqueTraslape("modal");
    validarJustificacion("modal");
    sincronizarResumenJustificacionModal();

    mostrarFeedbackContext("modal", "enlaceFeedback", "", "");
    mostrarFeedbackContext("modal", "lanFeedback", "", "");
    mostrarFeedbackContext("modal", "wanFeedback", "", "");

    actualizarEstadoGuardar("modal");

    const modalElement = getModalElement();
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
}

function setModalValue(id, value) {
    const el = getScopedElement("modal", id);
    if (el) el.value = value || "";
}

function confirmarEliminar(button) {
    const row = button.closest("tr");
    if (!row) return;

    const realId = row.dataset.realId;
    if (confirm("¿Está seguro de que desea eliminar este registro?")) {
        const deleteId = document.getElementById("deleteId");
        const deleteForm = document.getElementById("deleteForm");
        if (deleteId) deleteId.value = realId;
        if (deleteForm) deleteForm.submit();
    }
}

function toggleSortByColumn(colIndex, type = "text") {
    const table = document.getElementById("networksTable");
    if (!table) return;

    const tbody = table.querySelector("tbody");
    const rows = Array.from(tbody.querySelectorAll("tr"));

    const currentDir = sortStates[colIndex] === "asc" ? "asc" : "desc";
    const newDir = currentDir === "asc" ? "desc" : "asc";
    sortStates[colIndex] = newDir;

    rows.sort((a, b) => {
        let aVal = (a.cells[colIndex]?.innerText || "").trim();
        let bVal = (b.cells[colIndex]?.innerText || "").trim();

        if (type === "number") {
            const numA = parseFloat(aVal) || 0;
            const numB = parseFloat(bVal) || 0;
            return newDir === "asc" ? numA - numB : numB - numA;
        }

        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();

        return newDir === "asc"
            ? aVal.localeCompare(bVal, "es", { numeric: true })
            : bVal.localeCompare(aVal, "es", { numeric: true });
    });

    rows.forEach(row => tbody.appendChild(row));

    document.querySelectorAll(".sort-indicator").forEach(ind => ind.innerText = "↕");
    const activeIndicator = document.querySelector(`.sort-indicator[data-col="${colIndex}"]`);
    if (activeIndicator) {
        activeIndicator.innerText = newDir === "asc" ? "↑" : "↓";
    }

    currentPage = 1;
    aplicarFiltrosTabla();
}

function toggleSidebar() {
    document.body.classList.toggle("sidebar-collapsed");
}

function toggleConflictPanel(type) {
    const panel = getScopedElement("modal", `${type}ConflictPanel`);
    if (!panel) return;
    panel.classList.toggle("d-none");
}

function setConflictPanelState(context, type, hasConflict, explanation) {
    const btn = getScopedElement(context, `${type}InfoBtn`);
    const panel = getScopedElement(context, `${type}ConflictPanel`);
    const text = getScopedElement(context, `${type}ConflictExplanationText`);

    if (!btn || !panel || !text) return;

    text.textContent = explanation || "";

    if (hasConflict && explanation && explanation.trim() !== "") {
        btn.classList.remove("d-none");
    } else {
        btn.classList.add("d-none");
        panel.classList.add("d-none");
        text.textContent = "";
    }
}