#!/bin/bash
# 팀원의 접속 IP가 바뀌었을 때 AWS 보안그룹을 갱신하는 스크립트.
# 이름으로 태그해서 관리하므로, 같은 이름으로 다시 실행하면 그 사람의 예전 IP 규칙은
# 자동으로 지우고 새 IP로 교체한다 (보안그룹에 죽은 규칙이 계속 쌓이는 것을 방지).
#
# 사용법: ./update-aws-ip.sh <이름> <IP주소>
# 예시:   ./update-aws-ip.sh minsu 183.97.44.39

set -e

SG_ID="sg-03402b2178fae2852"
REGION="ap-northeast-2"
PORTS=(22 3307 8081)

NAME="$1"
IP="$2"

if [ -z "$NAME" ] || [ -z "$IP" ]; then
  echo "사용법: $0 <이름> <IP주소>"
  echo "예시:   $0 minsu 183.97.44.39"
  exit 1
fi

if ! [[ "$IP" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
  echo "올바른 IP 형식이 아닙니다: $IP"
  exit 1
fi

CIDR="${IP}/32"

for PORT in "${PORTS[@]}"; do
  # 같은 이름(Description)으로 등록된 기존 규칙, 그리고 지금 등록하려는 IP와 동일한
  # 규칙(설명 유무 무관)이 이미 있으면 둘 다 찾아서 제거 - AWS는 CIDR+포트가 같으면
  # description이 달라도 "중복 규칙"으로 보고 추가를 거부하기 때문
  STALE_CIDRS=$(aws ec2 describe-security-groups \
    --group-ids "$SG_ID" --region "$REGION" \
    --query "SecurityGroups[0].IpPermissions[?FromPort==\`$PORT\`].IpRanges[?Description=='$NAME' || CidrIp=='$CIDR'].CidrIp" \
    --output text)

  if [ -n "$STALE_CIDRS" ]; then
    for OC in $STALE_CIDRS; do
      aws ec2 revoke-security-group-ingress \
        --group-id "$SG_ID" --protocol tcp --port "$PORT" \
        --cidr "$OC" --region "$REGION" >/dev/null
      echo "포트 $PORT: 기존 규칙($OC) 제거"
    done
  fi

  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --region "$REGION" \
    --ip-permissions "IpProtocol=tcp,FromPort=$PORT,ToPort=$PORT,IpRanges=[{CidrIp=$CIDR,Description=$NAME}]" \
    >/dev/null
  echo "포트 $PORT: $NAME -> $CIDR 허용"
done

echo "완료: $NAME ($IP) 접근 갱신됨"
