<template>
  <q-page class="flex flex-center bg-grey-2">
    <div class="q-pa-md" style="max-width: 520px; width: 100%">

      <!-- Consultar inventario -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-h6 text-secondary">
            <q-icon name="search" class="q-mr-xs" />
            Consultar inventario
          </div>
          <div class="text-caption text-grey">GET /v1/inventory/{productId}</div>
        </q-card-section>

        <q-separator />

        <q-card-section>
          <div class="row q-gutter-sm items-start">
            <q-input
              v-model.number="queryProductId"
              type="number"
              label="ID del Producto"
              outlined
              dense
              class="col"
              :rules="[v => (v !== null && v > 0) || 'Mayor a 0']"
              hide-bottom-space
            />
            <q-btn
              label="Consultar"
              color="secondary"
              icon="search"
              :loading="queryLoading"
              unelevated
              @click="fetchInventory"
            />
          </div>
        </q-card-section>

        <!-- Resultado consulta -->
        <template v-if="inventory">
          <q-separator />
          <q-card-section class="q-pa-none">
            <q-list separator>
              <q-item>
                <q-item-section>
                  <q-item-label overline>Producto</q-item-label>
                  <q-item-label class="text-body1">
                    {{ inventory.productName ?? '—' }}
                    <q-badge color="grey-6" class="q-ml-xs">ID {{ inventory.productId }}</q-badge>
                  </q-item-label>
                </q-item-section>
              </q-item>
              <q-item>
                <q-item-section>
                  <q-item-label overline>Precio</q-item-label>
                  <q-item-label>{{ currency(inventory.productPrice) }}</q-item-label>
                </q-item-section>
                <q-item-section>
                  <q-item-label overline>Stock disponible</q-item-label>
                  <q-item-label :class="inventory.quantity < 10 ? 'text-warning text-weight-bold' : 'text-positive text-weight-bold'">
                    {{ inventory.quantity }} uds.
                    <q-icon v-if="inventory.quantity < 10" name="warning" color="warning" size="xs" />
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </template>

        <!-- Error consulta -->
        <template v-if="queryError">
          <q-separator />
          <q-card-section class="bg-negative text-white">
            <div class="text-weight-bold">{{ queryError.title }}</div>
            <div class="text-caption">{{ queryError.detail }}</div>
          </q-card-section>
        </template>
      </q-card>

      <!-- Formulario compra -->
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h6 text-primary">
            <q-icon name="shopping_cart" class="q-mr-xs" />
            Nueva compra
          </div>
          <div class="text-caption text-grey">POST /v1/inventory/purchase</div>
        </q-card-section>

        <q-separator />

        <q-card-section>
          <q-form ref="formRef" @submit.prevent="submit" class="q-gutter-md">

            <q-input
              v-model.number="form.productId"
              type="number"
              label="ID del Producto *"
              outlined
              dense
              :rules="[v => (v !== null && v > 0) || 'Requerido y mayor a 0']"
            />

            <q-input
              v-model.number="form.quantity"
              type="number"
              label="Cantidad *"
              outlined
              dense
              :rules="[v => (v !== null && v >= 1) || 'Mínimo 1']"
            />

            <q-input
              v-model="apiKey"
              label="API Key"
              outlined
              dense
              hint="Usa admin-key para operaciones de escritura"
            />

            <q-btn
              type="submit"
              label="Comprar"
              color="primary"
              icon="shopping_cart_checkout"
              :loading="loading"
              class="full-width"
              unelevated
            />

          </q-form>
        </q-card-section>
      </q-card>

      <!-- Resultado compra exitosa -->
      <q-card v-if="result" flat bordered class="q-mt-md">
        <q-card-section class="bg-positive text-white">
          <div class="text-subtitle1 text-weight-bold">
            <q-icon name="check_circle" class="q-mr-xs" />
            Compra registrada
          </div>
        </q-card-section>

        <q-card-section class="q-pa-none">
          <q-list separator>
            <q-item>
              <q-item-section>
                <q-item-label overline>Producto</q-item-label>
                <q-item-label class="text-body1">
                  {{ result.productName ?? '—' }}
                  <q-badge color="grey-6" class="q-ml-xs">ID {{ result.productId }}</q-badge>
                </q-item-label>
              </q-item-section>
            </q-item>

            <q-item>
              <q-item-section>
                <q-item-label overline>Precio unitario</q-item-label>
                <q-item-label>{{ currency(result.productPrice) }}</q-item-label>
              </q-item-section>
              <q-item-section>
                <q-item-label overline>Cantidad comprada</q-item-label>
                <q-item-label>{{ result.quantityPurchased }}</q-item-label>
              </q-item-section>
            </q-item>

            <q-item>
              <q-item-section>
                <q-item-label overline>Total pagado</q-item-label>
                <q-item-label class="text-h6 text-primary text-weight-bold">
                  {{ currency(result.totalPrice) }}
                </q-item-label>
              </q-item-section>
              <q-item-section>
                <q-item-label overline>Stock restante</q-item-label>
                <q-item-label :class="result.remainingQuantity < 10 ? 'text-warning text-weight-bold' : ''">
                  {{ result.remainingQuantity }} uds.
                  <q-icon v-if="result.remainingQuantity < 10" name="warning" color="warning" size="xs" />
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>

      <!-- Error compra -->
      <q-banner v-if="error" class="bg-negative text-white q-mt-md" rounded>
        <template #avatar>
          <q-icon name="error_outline" />
        </template>
        <div class="text-weight-bold">{{ error.title }}</div>
        <div class="text-caption">{{ error.detail }}</div>
      </q-banner>

    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { api } from 'boot/axios'

// --- Consulta de inventario ---
const queryProductId = ref(null)
const queryLoading = ref(false)
const inventory = ref(null)
const queryError = ref(null)

async function fetchInventory () {
  if (!queryProductId.value || queryProductId.value < 1) return

  queryLoading.value = true
  inventory.value = null
  queryError.value = null

  try {
    const { data } = await api.get(`/v1/inventory/${queryProductId.value}`, {
      headers: { Authorization: `Bearer ${apiKey.value}` }
    })
    inventory.value = data.data.attributes
  } catch (err) {
    const errors = err.response?.data?.errors
    queryError.value = errors?.length
      ? { title: errors[0].title, detail: errors[0].detail }
      : { title: 'Error de conexión', detail: 'No se pudo obtener el inventario.' }
  } finally {
    queryLoading.value = false
  }
}

// --- Compra ---
const formRef = ref(null)
const loading = ref(false)
const result = ref(null)
const error = ref(null)
const apiKey = ref('admin-key')

const form = ref({
  productId: null,
  quantity: null
})

async function submit () {
  const valid = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  result.value = null
  error.value = null

  try {
    const { data } = await api.post(
      '/v1/inventory/purchase',
      {
        data: {
          type: 'purchases',
          attributes: {
            productId: form.value.productId,
            quantity: form.value.quantity
          }
        }
      },
      {
        headers: {
          'Content-Type': 'application/vnd.api+json',
          Authorization: `Bearer ${apiKey.value}`
        }
      }
    )
    result.value = data.data.attributes
    // refresca el inventario si el productId coincide
    if (inventory.value?.productId === form.value.productId) {
      fetchInventory()
    }
  } catch (err) {
    const errors = err.response?.data?.errors
    error.value = errors?.length
      ? { title: errors[0].title, detail: errors[0].detail }
      : { title: 'Error de conexión', detail: 'No se pudo conectar con el servicio de inventario.' }
  } finally {
    loading.value = false
  }
}

// --- Utilidades ---
function currency (value) {
  if (value == null) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2
  }).format(value)
}
</script>