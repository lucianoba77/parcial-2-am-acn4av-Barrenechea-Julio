#!/bin/bash

# Script para verificar el estado del plan Blaze

echo "=== Verificando Estado del Plan Blaze ==="
echo ""

echo "1. Verificando proyecto actual..."
npx firebase projects:list | grep "mimedicinaapp"

echo ""
echo "2. Intentando desplegar funciones (esto mostrará el error si el plan no está activo)..."
echo ""

npx firebase deploy --only functions 2>&1 | grep -A 3 "Error\|Blaze\|plan" || echo "✓ No se encontraron errores relacionados con el plan"

echo ""
echo "3. Verifica manualmente en Firebase Console:"
echo "   https://console.firebase.google.com/project/mimedicinaapp/usage/details"
echo ""
echo "   Debe mostrar: 'Blaze (pay-as-you-go)'"
echo ""
echo "4. Verifica la cuenta de facturación en Google Cloud:"
echo "   https://console.cloud.google.com/billing?project=mimedicinaapp"
echo "   Debe mostrar una cuenta de facturación 'Activa'"
echo ""

