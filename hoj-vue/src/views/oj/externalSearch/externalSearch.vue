<template>
  <div>
    <el-row :gutter="30">
      <el-col :md="15" :sm="24">

        <el-card class="card-top">
          <div slot="header" class="clearfix title">
            <span class="home-title panel-title">
              {{ $t('m.Common_Websites') }}
            </span>
          </div>
          <el-row :gutter="20">
            <el-col
                :md="8"
                :sm="24"
                v-for="(oj, index) in remoteJudgeList"
                :key="index"
            >
              <a :href="oj.url" target="_blank">
                <el-tooltip :content="oj.name" placement="top">
                  <el-image
                      :src="oj.logo"
                      fit="fill"
                      class="oj-logo"
                      :class="
                      oj.status ? 'oj-normal ' + oj.name : 'oj-error ' + oj.name
                    "
                  >
                    <div slot="error" class="image-slot">
                      <i class="el-icon-picture-outline"></i>
                    </div>
                  </el-image>
                </el-tooltip>
              </a>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex';
import Avatar from 'vue-avatar';
const Announcements = () => import('@/components/oj/common/Announcements.vue');
export default {
  name: 'home',
  components: {
    Announcements,
    Avatar,
  },
  data() {
    return {
      interval: 5000,
      srcHight: '440px',
      remoteJudgeList: [
        {
          url: 'https://stackoverflow.com',
          name: 'StackOverFlow',
          logo: require('@/assets/stackoverflow.jpg'),
          status: true,
        },
        {
          url: 'https://github.com',
          name: 'Github',
          logo: require('@/assets/github.jpg'),
          status: true,
        },
        {
          url: 'https://www.nowcoder.com/',
          name: 'Nowcoder',
          logo: require('@/assets/nowcoder.jpg'),
          status: true,
        },
        {
          url: 'https://leetcode-cn.com/',
          name: 'Leetcode',
          logo: require('@/assets/leetcode.jpg'),
          status: true,
        },

      ],
    };
  },
  mounted() {
    let screenWidth = window.screen.width;
    if (screenWidth < 768) {
      this.srcHight = '200px';
    } else {
      this.srcHight = '440px';
    }
    this.CONTEST_STATUS_REVERSE = Object.assign({}, CONTEST_STATUS_REVERSE);
    this.CONTEST_TYPE_REVERSE = Object.assign({}, CONTEST_TYPE_REVERSE);
    this.getHomeCarousel();
  },
  methods: {

  },
  computed: {
    ...mapState(['websiteConfig']),
    ...mapGetters(['isAuthenticated']),
  },
};
</script>
<style scoped>
.card-top {
  margin-top: 20px;
}
.oj-logo {
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  margin-bottom: 1rem;
  padding: 0.5rem 1rem;
  background: rgb(255, 255, 255);
  min-height: 47px;
}

.el-carousel__item h3 {
  color: #475669;
  font-size: 14px;
  opacity: 0.75;
  line-height: 200px;
  margin: 0;
}

ul,
li {
  padding: 0;
  margin: 0;
  list-style: none;
}
.contest-info li {
  display: inline-block;
  padding-right: 10px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: '';
}
.clearfix:after {
  clear: both;
}

.clearfix h2 {
  color: #409eff;
}
@media screen and (min-width: 1050px) {
  /deep/ .vxe-table--body-wrapper {
    overflow-x: hidden !important;
  }
}
</style>