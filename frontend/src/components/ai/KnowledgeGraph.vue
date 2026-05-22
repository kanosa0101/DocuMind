<template>
  <div class="knowledge-graph">
    <div class="graph-header">
      <span class="graph-icon">🌐</span>
      <span>知识图谱</span>
    </div>
    <div class="graph-container">
      <svg ref="svgRef" class="graph-svg" :width="width" :height="height">
        <!-- 连接线 -->
        <g class="edges">
          <line
            v-for="edge in edges"
            :key="edge.id"
            :x1="edge.source.x"
            :y1="edge.source.y"
            :x2="edge.target.x"
            :y2="edge.target.y"
            class="edge-line"
            :class="{ active: edge.active }"
          />
        </g>
        <!-- 节点 -->
        <g class="nodes">
          <g
            v-for="node in nodes"
            :key="node.id"
            :transform="`translate(${node.x}, ${node.y})`"
            class="node-group"
            @mouseenter="handleNodeHover(node)"
            @mouseleave="handleNodeLeave(node)"
          >
            <circle
              class="node-circle"
              :r="node.size"
              :class="[node.type, { active: node.active }]"
            />
            <text
              class="node-label"
              :y="node.size + 4"
              text-anchor="middle"
            >{{ node.label }}</text>
          </g>
        </g>
      </svg>
      <!-- Tooltip -->
      <div v-if="tooltip.visible" class="node-tooltip glass-card" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
        <h4>{{ tooltip.title }}</h4>
        <p>{{ tooltip.content }}</p>
      </div>
    </div>
    <p v-if="nodes.length === 0" class="empty">等待知识图谱生成...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'

interface GraphNode {
  id: string
  label: string
  type: 'concept' | 'entity' | 'keyword'
  size: number
  x: number
  y: number
  active: boolean
}

interface GraphEdge {
  id: string
  source: GraphNode
  target: GraphNode
  active: boolean
}

const props = defineProps<{
  data?: { nodes: any[], edges: any[] }
}>()

const width = ref(400)
const height = ref(300)
const nodes = ref<GraphNode[]>([])
const edges = ref<GraphEdge[]>([])
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  title: '',
  content: ''
})

// Animation state
let animationId: number | null = null
const velocities = ref<Map<string, { vx: number, vy: number }>>(new Map())

// Simple force-directed layout simulation
function initializeGraph() {
  if (!props.data) return

  const centerX = width.value / 2
  const centerY = height.value / 2

  // Convert data to internal format
  nodes.value = props.data.nodes.map((n, i) => {
    const angle = (i / props.data!.nodes.length) * Math.PI * 2
    const radius = 80 + Math.random() * 40
    return {
      id: n.id || `node-${i}`,
      label: n.label || n.name || '',
      type: n.type || 'keyword',
      size: n.size || (n.type === 'concept' ? 16 : n.type === 'entity' ? 12 : 8),
      x: centerX + Math.cos(angle) * radius,
      y: centerY + Math.sin(angle) * radius,
      active: false
    }
  })

  edges.value = props.data.edges.map((e, i) => {
    const source = nodes.value.find(n => n.id === e.source) || nodes.value[0]
    const target = nodes.value.find(n => n.id === e.target) || nodes.value[1]
    return {
      id: `edge-${i}`,
      source,
      target,
      active: false
    }
  })

  // Initialize velocities
  nodes.value.forEach(node => {
    velocities.value.set(node.id, { vx: 0, vy: 0 })
  })

  startAnimation()
}

function startAnimation() {
  if (animationId) return

  const centerX = width.value / 2
  const centerY = height.value / 2

  animationId = requestAnimationFrame(() => {
    animate(centerX, centerY)
  })
}

function animate(centerX: number, centerY: number) {
  // Simple force simulation
  nodes.value.forEach(node => {
    const vel = velocities.value.get(node.id) || { vx: 0, vy: 0 }

    // Repulsion from other nodes
    nodes.value.forEach(other => {
      if (node.id === other.id) return
      const dx = node.x - other.x
      const dy = node.y - other.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 60) {
        const force = (60 - dist) * 0.05
        vel.vx += (dx / dist) * force
        vel.vy += (dy / dist) * force
      }
    })

    // Attraction to connected nodes
    edges.value.forEach(edge => {
      if (edge.source.id === node.id || edge.target.id === node.id) {
        const other = edge.source.id === node.id ? edge.target : edge.source
        const dx = other.x - node.x
        const dy = other.y - node.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 80) {
          const force = (dist - 80) * 0.02
          vel.vx += (dx / dist) * force
          vel.vy += (dy / dist) * force
        }
      }
    })

    // Attraction to center
    vel.vx += (centerX - node.x) * 0.01
    vel.vy += (centerY - node.y) * 0.01

    // Damping
    vel.vx *= 0.95
    vel.vy *= 0.95

    // Update position
    node.x += vel.vx
    node.y += vel.vy

    // Boundary check
    node.x = Math.max(node.size + 10, Math.min(width.value - node.size - 10, node.x))
    node.y = Math.max(node.size + 10, Math.min(height.value - node.size - 10, node.y))

    velocities.value.set(node.id, vel)
  })

  animationId = requestAnimationFrame(() => animate(centerX, centerY))
}

function stopAnimation() {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

function handleNodeHover(node: GraphNode) {
  node.active = true
  // Highlight connected edges
  edges.value.forEach(edge => {
    edge.active = edge.source.id === node.id || edge.target.id === node.id
  })
  // Show tooltip
  tooltip.value = {
    visible: true,
    x: node.x + 20,
    y: node.y - 20,
    title: node.label,
    content: `类型: ${node.type}`
  }
}

function handleNodeLeave(node: GraphNode) {
  node.active = false
  edges.value.forEach(edge => {
    edge.active = edge.source.id === node.id || edge.target.id === node.id
  })
  tooltip.value.visible = false
}

watch(() => props.data, () => {
  initializeGraph()
}, { immediate: true })

onMounted(() => {
  initializeGraph()
})

onUnmounted(() => {
  stopAnimation()
})
</script>

<style scoped>
.knowledge-graph {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--color-text);
}

.graph-container {
  position: relative;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

[data-theme="dark"] .graph-container {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.graph-svg {
  display: block;
}

/* Edges */
.edge-line {
  stroke: rgba(8, 145, 178, 0.3);
  stroke-width: 1;
  transition: all var(--transition-fast);
}

[data-theme="dark"] .edge-line {
  stroke: rgba(8, 145, 178, 0.25);
}

.edge-line.active {
  stroke: var(--aurora-cyan);
  stroke-width: 2;
}

[data-theme="dark"] .edge-line.active {
  stroke-width: 3;
  filter: drop-shadow(0 0 4px rgba(8, 145, 178, 0.6));
}

/* Nodes */
.node-circle {
  fill: var(--aurora-cyan);
  transition: all var(--transition-fast);
  cursor: pointer;
}

[data-theme="dark"] .node-circle {
  filter: drop-shadow(0 0 6px rgba(8, 145, 178, 0.4));
}

.node-circle.concept {
  fill: var(--aurora-emerald);
}

[data-theme="dark"] .node-circle.concept {
  filter: drop-shadow(0 0 8px rgba(16, 185, 129, 0.5));
}

.node-circle.entity {
  fill: var(--aurora-blue);
}

[data-theme="dark"] .node-circle.entity {
  filter: drop-shadow(0 0 6px rgba(59, 130, 246, 0.4));
}

.node-circle.keyword {
  fill: var(--aurora-cyan);
}

.node-circle.active {
  r: 20;
  fill: var(--aurora-cyan);
  filter: drop-shadow(0 0 12px rgba(8, 145, 178, 0.8));
}

[data-theme="dark"] .node-circle.active {
  filter: drop-shadow(0 0 15px rgba(8, 145, 178, 0.9));
}

.node-label {
  font-size: 10px;
  fill: var(--color-text);
  pointer-events: none;
}

[data-theme="dark"] .node-label {
  fill: var(--color-text);
}

/* Tooltip */
.node-tooltip {
  position: absolute;
  padding: 8px 12px;
  font-size: var(--font-size-sm);
  z-index: 100;
  pointer-events: none;
  animation: tooltip-fade 0.2s ease;
}

[data-theme="dark"] .node-tooltip {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

.node-tooltip h4 {
  font-size: var(--font-size-sm);
  margin-bottom: 4px;
  color: var(--aurora-cyan);
}

[data-theme="dark"] .node-tooltip h4 {
  text-shadow: var(--glow-text-cyan);
}

.node-tooltip p {
  color: var(--color-text-muted);
}

@keyframes tooltip-fade {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}

.empty {
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px;
}
</style>