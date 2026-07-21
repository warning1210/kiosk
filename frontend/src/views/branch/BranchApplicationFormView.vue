<template>
  <!-- 예비 지점장이 이메일의 일회용 URL로 접속하는 전용 지점 개설 신청서다. -->
  <BranchJoinView />
</template>

<script setup>
// 이름·지점명·연락처·주소·사업자번호·계정 정보를 받는 기존 신청서 본문을 재사용한다.
import BranchJoinView from './BranchJoinView.vue'
</script>
