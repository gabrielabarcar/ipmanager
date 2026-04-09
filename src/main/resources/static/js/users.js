function limpiarFormularioUsuario() {
    document.getElementById("id").value = "";
    document.getElementById("username").value = "";
    document.getElementById("nombre").value = "";
    document.getElementById("apellido1").value = "";
    document.getElementById("apellido2").value = "";
    document.getElementById("correo").value = "";
    document.getElementById("password").value = "";
    document.getElementById("confirmPassword").value = "";

    const rol = document.getElementById("rol");
    if (rol) {
        rol.value = "VIEWER";
    }

    document.getElementById("enabled").checked = true;

    document.querySelectorAll('select[name*=".accessLevel"]').forEach(select => {
        select.value = "NONE";
    });
}

function editarUsuario(id, username, nombre, apellido1, apellido2, correo, rol, enabled) {
    document.getElementById("id").value = id || "";
    document.getElementById("username").value = username || "";
    document.getElementById("nombre").value = nombre || "";
    document.getElementById("apellido1").value = apellido1 || "";
    document.getElementById("apellido2").value = apellido2 || "";
    document.getElementById("correo").value = correo || "";

    const rolInput = document.getElementById("rol");
    if (rolInput) {
        rolInput.value = rol || "VIEWER";
    }

    document.getElementById("enabled").checked = enabled === true || enabled === "true";

    // MUY IMPORTANTE: al editar, contraseña vacía si no se desea cambiar
    document.getElementById("password").value = "";
    document.getElementById("confirmPassword").value = "";

    window.scrollTo({ top: 0, behavior: "smooth" });
}

function validarFormularioUsuario() {
    const password = document.getElementById("password").value;
    const confirm = document.getElementById("confirmPassword").value;

    // solo validar coincidencia si el usuario escribió algo en contraseña
    if (password && password !== confirm) {
        alert("Las contraseñas no coinciden");
        return false;
    }

    return true;
}

document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");

    if (form) {
        form.addEventListener("submit", function (e) {
            if (!validarFormularioUsuario()) {
                e.preventDefault();
            }
        });
    }

    const botonesEditar = document.querySelectorAll(".editar-usuario-btn");
    botonesEditar.forEach(function (btn) {
        btn.addEventListener("click", function () {
            editarUsuario(
                btn.dataset.id,
                btn.dataset.username,
                btn.dataset.nombre,
                btn.dataset.apellido1,
                btn.dataset.apellido2,
                btn.dataset.correo,
                btn.dataset.role,
                btn.dataset.enabled
            );
        });
    });
});