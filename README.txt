# 🍽️ Bombay Core - Restaurant Management System

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

Sistema SaaS multi-tenant completo para gestión de restaurantes con facturación, inventario, cierre diario y reportes.

## 🚀 Características

### ✅ Módulos Implementados
- **🔐 Seguridad Multi-tenant** - Autenticación por subdominio
- **📦 Inventario Completo** - Productos, ingredientes, recetas, control de stock
- **💰 Facturación/Ventas** - Sistema POS completo con impuestos
- **🛒 Compras** - Gestión de proveedores y actualización automática de inventario
- **📊 Cierre Diario** - Flujo automatizado de 2 pasos con validación
- **📈 Reportes** - Consumo diario, cálculo de merma, ventas por período

### 🏢 Arquitectura
- **Backend:** Spring Boot 3.5.8 + Spring Security 6 + JPA/Hibernate
- **Frontend:** Thymeleaf + Bootstrap 5.3
- **Base de datos:** MySQL 8.0 con multi-tenancy por columna `empresa_id`
- **Java:** 17

## 📋 Requisitos

- Java 17+
- MySQL 8.0+
- Maven 3.8+

## 🛠️ Instalación

1. **Clonar repositorio:**
```bash
git clone https://github.com/TU_USUARIO/bombay-core-restaurant-system.git
cd bombay-core-restaurant-system
