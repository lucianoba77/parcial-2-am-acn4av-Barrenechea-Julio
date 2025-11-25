#!/bin/bash

# Script para desplegar Cloud Functions de Firebase
# Control Medicamentos

echo "=== Despliegue de Cloud Functions de Firebase ==="
echo ""

# Verificar si Firebase CLI está disponible
if ! command -v firebase &> /dev/null && ! command -v npx &> /dev/null; then
    echo "Error: Firebase CLI no está instalado."
    echo "Instalando Firebase CLI localmente..."
    npm install --save-dev firebase-tools
fi

# Usar npx si firebase no está en PATH
FIREBASE_CMD="firebase"
if ! command -v firebase &> /dev/null; then
    FIREBASE_CMD="npx firebase"
fi

echo "1. Verificando proyecto Firebase..."
$FIREBASE_CMD projects:list

echo ""
echo "2. Seleccionando proyecto 'mimedicinaapp'..."
$FIREBASE_CMD use mimedicinaapp

echo ""
echo "3. Instalando dependencias de Functions..."
cd functions
npm install
cd ..

echo ""
echo "4. Configurando credenciales de Google OAuth..."
echo "   IMPORTANTE: Necesitas obtener el Client Secret de Google Cloud Console"
echo "   URL: https://console.cloud.google.com/apis/credentials?project=mimedicinaapp"
echo ""
read -p "¿Tienes el Client Secret? (s/n): " tiene_secret

if [ "$tiene_secret" != "s" ] && [ "$tiene_secret" != "S" ]; then
    echo ""
    echo "Por favor obtén el Client Secret primero:"
    echo "1. Ve a: https://console.cloud.google.com/apis/credentials?project=mimedicinaapp"
    echo "2. Busca tu OAuth 2.0 Client ID (debe ser: 938306231220-eglguura8gmu64thg88pf5r4cbc59oj3.apps.googleusercontent.com)"
    echo "3. Haz clic en el cliente"
    echo "4. Copia el 'Client Secret'"
    echo ""
    read -p "Presiona Enter cuando tengas el Client Secret..."
fi

echo ""
read -p "Ingresa el Client Secret: " client_secret

if [ -z "$client_secret" ]; then
    echo "Error: Client Secret no puede estar vacío"
    exit 1
fi

CLIENT_ID="938306231220-eglguura8gmu64thg88pf5r4cbc59oj3.apps.googleusercontent.com"

echo ""
echo "5. Configurando credenciales en Firebase Functions..."
$FIREBASE_CMD functions:config:set google.client_id="$CLIENT_ID" google.client_secret="$client_secret"

echo ""
echo "6. Verificando configuración..."
$FIREBASE_CMD functions:config:get

echo ""
read -p "¿Deseas desplegar las funciones ahora? (s/n): " desplegar

if [ "$desplegar" = "s" ] || [ "$desplegar" = "S" ]; then
    echo ""
    echo "7. Desplegando Cloud Functions..."
    $FIREBASE_CMD deploy --only functions
    
    echo ""
    echo "=== Despliegue completado ==="
    echo ""
    echo "Las funciones están desplegadas. Puedes verificar en:"
    echo "https://console.firebase.google.com/project/mimedicinaapp/functions"
else
    echo ""
    echo "Para desplegar más tarde, ejecuta:"
    echo "$FIREBASE_CMD deploy --only functions"
fi

