<template>
  <div>
    <component :is="currentView"></component>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { RULE_TYPE } from '@/common/constants';
const ACMPaperRank = () => import('./ACMPaperRank.vue');
const OIPaperRank = () => import('./OIPaperRank.vue');
const NullComponent = {
  name: 'null-component',
  template: '<div></div>',
};

export default {
  name: 'paper-rank',
  components: {
    ACMPaperRank,
    OIPaperRank,
    NullComponent,
  },
  beforeCreate() {
    if (this.$store.state.paper.paperProblems.length === 0) {
      this.$store.dispatch('getPaperProblems');
    }
  },
  computed: {
    ...mapGetters(['paperRuleType']),
    currentView() {
      if (this.paperRuleType === null) {
        return 'NullComponent';
      }
      return this.paperRuleType === RULE_TYPE.ACM
        ? 'ACMPaperRank'
        : 'OIPaperRank';
    },
  },
  beforeRouteLeave(to, from, next) {
    this.$store.commit('changePaperItemVisible', { menu: true });
    next();
  },
};
</script>
<style></style>
