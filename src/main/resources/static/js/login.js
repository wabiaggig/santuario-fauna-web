const clave = document.getElementById('clave');
const mostrarClave = document.querySelector('.mostrar-clave');

if (clave && mostrarClave) {
    mostrarClave.hidden = false;
    mostrarClave.addEventListener('click', () => {
        const visible = clave.type === 'password';
        clave.type = visible ? 'text' : 'password';
        mostrarClave.textContent = visible ? 'Ocultar' : 'Mostrar';
        mostrarClave.setAttribute('aria-pressed', String(visible));
    });
}
