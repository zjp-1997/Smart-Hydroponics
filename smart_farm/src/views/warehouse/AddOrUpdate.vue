<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { addWarehouseItem, getWarehouseItemById, listWarehouseFarmOwners, listWarehouseOperatorOptions, updateWarehouseItem, type WarehouseItem, type WarehouseOperatorOption } from '@/api/warehouse'
import { uploadManagedImage } from '@/api/managedImage'
import { getCurrentUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()
const formRef = ref<FormInstance>(); const detailLoading = ref(false); const submitLoading = ref(false)
// 图片上传使用独立状态，避免上传期间重复选择文件或提前提交表单。
const uploadLoading = ref(false)
const operatorOptions = ref<WarehouseOperatorOption[]>([])
// 农场主归属与入库人使用两个独立列表，避免误把入库人当作物资所有者。
const farmOwnerOptions = ref<WarehouseOperatorOption[]>([])
type WarehouseItemForm = WarehouseItem
// 新增物资不接收手工编码，编码在后端取得数据库编号后自动生成。
const emptyForm = (): WarehouseItemForm => ({ itemName: '', imageUrl: '', category: 1, specification: '', unit: '', stockQty: 1, initialUnitPrice: undefined, warningQty: 0, manufacturer: '', status: 1, remark: '' })
const form = reactive<WarehouseItemForm>(emptyForm())
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const isEdit = computed(() => props.id != null)
const currentUser = computed(() => getCurrentUser())
// 分类默认图由 smart_farm 静态目录提供，业务上传图则通过后端地址访问。
const imagePreviewUrl = computed(() => {
  const imageUrl = form.imageUrl?.trim()
  if (!imageUrl) return ''
  return imageUrl.startsWith('/warehouse-images/') ? imageUrl : getFileUrl(imageUrl)
})
const categoryMap: Record<number, string> = { 1: '种子', 2: '肥料', 3: '农药', 4: '工具', 5: '设备耗材', 6: '其他' }
const rules: FormRules<WarehouseItemForm> = { imageUrl: [{ required: true, whitespace: true, message: '请上传物资图片', trigger: 'change' }], itemName: [{ required: true, message: '请输入物资名称', trigger: 'blur' }], category: [{ required: true, message: '请选择物资分类', trigger: 'change' }], unit: [{ required: true, message: '请输入单位', trigger: 'blur' }], initialUnitPrice: [{ required: true, type: 'number', min: 0, message: '请输入不小于0的入库单价', trigger: 'change' }], farmOwnerId: [{ required: true, message: '请选择所属农场主', trigger: 'change' }], inboundOperatorId: [{ required: true, message: '请选择入库人', trigger: 'change' }] }
watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  Object.assign(form, emptyForm(), { inboundOperatorId: currentUser.value?.id ?? currentUser.value?.adminId }); formRef.value?.clearValidate(); detailLoading.value = true
  try {
    const [operators, owners] = await Promise.all([listWarehouseOperatorOptions(), listWarehouseFarmOwners()])
    operatorOptions.value = operators.data || []
    farmOwnerOptions.value = owners.data || []
    // 农场主登录时直接使用自己的归属；管理员必须明确选择。
    if (!isEdit.value && farmOwnerOptions.value.length === 1 && farmOwnerOptions.value[0]?.id === currentUser.value?.id) {
      form.farmOwnerId = currentUser.value?.id
    }
    if (props.id != null) Object.assign(form, (await getWarehouseItemById(props.id)).data)
  } finally { detailLoading.value = false }
})

/** 在浏览器侧提前拦截不支持或过大的图片，后端仍会执行同等安全校验。 */
const validateImage = (file: File) => {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG 或 WEBP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 上传成功后只保存后端返回的相对地址，列表页和编辑弹框统一解析该地址。 */
const handleImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file || !validateImage(file)) return

  uploadLoading.value = true
  try {
    const result = await uploadManagedImage(file, 'warehouse')
    form.imageUrl = result.data.imageUrl
    void formRef.value?.validateField('imageUrl')
    ElMessage.success('物资图片上传成功')
  } finally {
    uploadLoading.value = false
  }
}

/** 清空图片并立即显示必填错误。 */
const removeImage = () => {
  form.imageUrl = ''
  void formRef.value?.validateField('imageUrl')
}

const submit = async () => {
  if (uploadLoading.value) { ElMessage.warning('图片正在上传，请稍候'); return }
  if (!(await formRef.value?.validate().catch(() => false))) return
  if (!isEdit.value && Number(form.stockQty || 0) <= 0) { ElMessage.warning('入库数量必须大于0'); return }
  submitLoading.value = true
  try {
    if (props.id != null) {
      await updateWarehouseItem({ ...form, id: props.id })
    } else {
      // 即使响应式表单残留编辑态编码，新增请求也不把编码发送给后端。
      const createPayload = { ...form }
      delete createPayload.itemCode
      await addWarehouseItem({ ...createPayload, initialUnitPrice: form.initialUnitPrice! })
    }
    ElMessage.success(isEdit.value ? '编辑物资成功' : '物资入库成功')
    dialogVisible.value = false
    emit('success')
  }
  finally { submitLoading.value = false }
}
</script>
<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物资' : '新增物资入库'" width="560px" destroy-on-close>
    <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="物资图片" prop="imageUrl">
        <div class="image-field">
          <el-upload
            v-loading="uploadLoading"
            class="warehouse-image-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :disabled="uploadLoading"
            :on-change="handleImageChange"
          >
            <img v-if="imagePreviewUrl" class="upload-image" :src="imagePreviewUrl" alt="物资图片预览" />
            <span v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <small>上传图片</small>
            </span>
          </el-upload>
          <div class="image-help">
            <span>支持 JPG、PNG、WEBP，大小不超过 5MB</span>
            <el-button v-if="form.imageUrl" type="danger" link :icon="Delete" @click="removeImage">移除图片</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="物资名称" prop="itemName"><el-input v-model="form.itemName" placeholder="请输入物资名称" /></el-form-item>
      <el-form-item label="所属农场主" prop="farmOwnerId">
        <el-select v-model="form.farmOwnerId" filterable placeholder="请选择所属农场主">
          <el-option v-for="owner in farmOwnerOptions" :key="owner.id" :label="owner.nickname || owner.username" :value="owner.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="category"><el-select v-model="form.category"><el-option v-for="(label, value) in categoryMap" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
      <el-form-item label="规格型号"><el-input v-model="form.specification" placeholder="请输入规格型号" /></el-form-item>
      <el-form-item label="单位" prop="unit"><el-input v-model="form.unit" placeholder="请输入单位" /></el-form-item>
      <el-form-item :label="isEdit ? '当前库存' : '入库数量'" prop="stockQty">
        <el-input-number v-model="form.stockQty" :min="0.01" :disabled="isEdit" />
        <span v-if="isEdit" class="field-help">库存变更请使用入库、出库或库存调整功能</span>
      </el-form-item>
      <el-form-item label="入库单价" prop="initialUnitPrice">
        <el-input-number v-model="form.initialUnitPrice" :min="0" :precision="2" :step="0.01" placeholder="请输入单价（元）" />
        <span class="field-help">{{ isEdit ? '修改后同步更新首次入库流水金额' : '用于生成首次入库流水和入库金额' }}</span>
      </el-form-item>
      <el-form-item label="入库人" prop="inboundOperatorId">
        <el-select v-model="form.inboundOperatorId" filterable placeholder="请选择入库人">
          <el-option v-for="operator in operatorOptions" :key="operator.id" :label="operator.nickname || operator.username" :value="operator.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="预警数量"><el-input-number v-model="form.warningQty" :min="0" /></el-form-item>
      <el-form-item label="生产厂家"><el-input v-model="form.manufacturer" placeholder="请输入生产厂家" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" :disabled="uploadLoading" @click="submit">{{ isEdit ? '保存' : '确认入库' }}</el-button></template>
  </el-dialog>
</template>
<style scoped>
:deep(.el-select), :deep(.el-input-number) { width: 100%; }

.field-help { display: block; width: 100%; margin-top: 4px; color: #64748b; font-size: 12px; line-height: 1.5; }

/* 上传区保持在原有表单栅格内，不改变弹框宽度和其他字段布局。 */
.image-field {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
}

.warehouse-image-uploader :deep(.el-upload) {
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.warehouse-image-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #ffffff;
}

.upload-placeholder,
.image-help {
  display: flex;
  flex-direction: column;
}

.upload-placeholder {
  align-items: center;
  gap: 6px;
  color: #64748b;
}

.upload-placeholder .el-icon { font-size: 22px; }
.upload-placeholder small { font-size: 12px; }

.image-help {
  align-items: flex-start;
  gap: 8px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
