<template>
  <comment :cid="$route.params.paperID"></comment>
</template>
<script>
import comment from '@/components/oj/comment/comment.vue';
export default {
  name: 'PaperComment',
  components: {
    comment,
  },
};
</script>
