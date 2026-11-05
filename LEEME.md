# Parche Zenkai para MojoLauncher (v6)
Copia app_mojo encima de tu proyecto y compila; carga layout_zenkai_mobile_styled.json.

v6: los menus ya no se encimen con los botones.
- Causa: los botones tienen translationZ=10 y el menu tenia 0, asi que Android dibujaba los botones ENCIMA del menu. Ahora el menu tiene mayor elevacion.
- Ademas: el menu se coloca arriba/abajo/derecha/izquierda del boton, donde tape menos botones, y los que queden debajo se ocultan mientras esta abierto.
- Shortcuts (boton Menu): oculta los botones bajo la ventana y los devuelve al cerrarla.
v5: arreglo de teclas GLFW/Android. Sin resolver: Ki blast y Transform.
No compilado aqui (sin SDK ni red).
