# Parche Zenkai para MojoLauncher (hecho sobre el código del zip que subiste)

Archivos (copiar encima del proyecto, mismas rutas):
- ControlData.java    -> campos zenkaiIcone / zenkaiEstilo / zenkaiContextos (se guardan y copian con el layout)
- ControlButton.java  -> dibuja icono + etiqueta de 1 línea pequeña (estilo "tile", "chip" o "icon"); el fondo/borde sigue siendo el de Mojo
- ControlLayout.java / ControlInterface.java -> visibilidad por contexto (container, inventario, dialogo...) según la pantalla que reporta ModUi Bridge
- ModUiServer.java    -> avisa al layout cuando cambia la pantalla

Luego cargar layout_zenkai_mobile_styled.json (tu mismo layout con los campos nuevos).

Notas:
- NO usar el ZenkaiLayoutUpdate anterior encima de este proyecto: borra el botón Atajos (-11, el que usa "Menu") y llama a ModUiControlButton, que no existe aquí.
- No lo compilé (sin Android SDK ni red aquí): lo revisé a mano, llaves/paréntesis balanceados. Compila con tu workflow de GitHub y mándame el error si sale alguno.
- Chat y Ataques de Ki tienen keycode 0 en tu JSON: se ven bien pero no hacen nada hasta asignarles una tecla.
- Los botones Fechar extra los asigné a contextos por suposición (container / habilidades+atributos / dialogo); se editan en el JSON.
