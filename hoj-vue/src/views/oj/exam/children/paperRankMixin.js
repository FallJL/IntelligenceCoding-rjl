import api from '@/common/api'
import { mapGetters, mapState } from 'vuex'
import { PAPER_STATUS, buildPaperRankConcernedKey } from '@/common/constants'
import storage from '@/common/storage';
export default {
  methods: {
    initConcernedList(){
      let key = buildPaperRankConcernedKey(this.$route.params.paperID);
      this.concernedList = storage.get(key) || [];
    },
    getPaperRankData (page = 1, refresh = false) {
      if (this.showChart && !refresh) {
        this.$refs.chart.showLoading({maskColor: 'rgba(250, 250, 250, 0.8)'})
      }
      let data = {
        currentPage:page,
        limit: this.limit,
        cid: this.$route.params.paperID,
        forceRefresh: this.forceUpdate ? true: false,
        removeStar: !this.showStarUser,
        concernedList:this.concernedList
      }
      api.getPaperRank(data).then(res => {
        if (this.showChart && !refresh) {
          this.$refs.chart.hideLoading()
        }
        this.total = res.data.data.total
        if (page === 1) {
          this.applyToChart(res.data.data.records)
        }
        this.applyToTable(res.data.data.records)
      })
    },
    handleAutoRefresh (status) {
      if (status == true) {
        this.refreshFunc = setInterval(() => {
          this.$store.dispatch('getPaperProblems');
          this.getPaperRankData(this.page, true);
        }, 10000)
      } else {
        clearInterval(this.refreshFunc)
      }
    },
    updateConcernedList(uid, isConcerned){
      if(isConcerned){
        this.concernedList.push(uid);
      }else{
        var index = this.concernedList.indexOf(uid);
        if (index > -1) {
        this.concernedList.splice(index, 1);
        }
      }
      let key = buildPaperRankConcernedKey(this.paperID);
      storage.set(key, this.concernedList);
      this.getPaperRankData(this.page, true);
    }
  },
  computed: {
    ...mapGetters(['isPaperAdmin','userInfo']),
    ...mapState({
      'paper': state => state.paper.paper,
      'paperProblems': state => state.paper.paperProblems
    }),
    showChart: {
      get () {
        return this.$store.state.paper.itemVisible.chart
      },
      set (value) {
        this.$store.commit('changePaperItemVisible', {chart: value})
      }
    },
    showStarUser:{
      get () {
        return !this.$store.state.paper.removeStar
      },
      set (value) {
        this.$store.commit('changeRankRemoveStar', {value: !value})
      }
    },
    showTable: {
      get () {
        return this.$store.state.paper.itemVisible.table
      },
      set (value) {
        this.$store.commit('changePaperItemVisible', {table: value})
      }
    },
    forceUpdate: {
      get () {
        return this.$store.state.paper.forceUpdate
      },
      set (value) {
        this.$store.commit('changeRankForceUpdate', {value: value})
      }
    },
    concernedList:{
      get () {
        return this.$store.state.paper.concernedList
      },
      set (value) {
        this.$store.commit('changeConcernedList', {value: value})
      }
    },
    refreshDisabled () {
      return this.paper.status == PAPER_STATUS.ENDED
    }
  },
  beforeDestroy () {
    clearInterval(this.refreshFunc)
  }
}
