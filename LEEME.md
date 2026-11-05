# Parche Zenkai para MojoLauncher (v5: arregla teclas equivocadas)

Copia app_mojo encima de tu proyecto y compila; carga layout_zenkai_mobile_styled.json.

BUG ARREGLADO (modui/ModUiInput.java + LayoutConverter.keyCodeFromGLFW ahora public):
el bridge manda codigos GLFW, pero PLATFORM.sendKeyEvent espera codigos ANDROID. Se enviaban tal cual:
GLFW 50 ("2") llegaba como Android 50 = V (estadisticas). Ahora se convierte. Tambien se arreglo:
- botones del mouse (GLFW 0/1/2 -> MotionEvent PRIMARY/SECONDARY/TERTIARY; antes clic derecho salia como izquierdo)
- teclas modificadoras (Alt/Ctrl/Shift) enviadas con codigo GLFW.
Esto tambien corrige el menu Shortcuts (ModUiDialog), que usa la misma clase.

Layout: Dash, Charge Ki, Focus, Fly, Jump, Sneak y Boost ahora usan zenkaiBind (la tecla real del juego); si no hay bridge usan el keycode de antes.
Si un bind no existe o esta sin asignar, sale un aviso "Bind not found / unbound: <nombre>" una vez.

Sin resolver: Ki blast (sin bind) y Transform (action_key es suposicion).
No compilado aqui (sin SDK ni red).
