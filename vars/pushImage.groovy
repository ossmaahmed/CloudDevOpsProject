// Push Image stage: log in to ECR with the instance role and push the image.
// Usage: pushImage(image: '...', registry: '<account>.dkr.ecr.<region>.amazonaws.com', region: 'us-east-1')
def call(Map cfg = [:]) {
    sh """
        aws ecr get-login-password --region ${cfg.region} | docker login --username AWS --password-stdin ${cfg.registry}
        docker push ${cfg.image}
    """
}
