resource "aws_security_group" "pipeline_sg" {
  name        = "pipeline-sg"
  description = "Security group for data eng pipeline"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Kafka"
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Kafka UI"
    from_port   = 8090
    to_port     = 8090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "pipeline_ec2" {
  ami                    = "ami-0a606d8395a538502"
  instance_type          = "t3.micro"
  key_name               = aws_key_pair.pipeline_key.key_name
  vpc_security_group_ids = [aws_security_group.pipeline_sg.id]

  tags = {
    Name = "data-eng-pipeline-dev-ec2"
  }
}

resource "aws_key_pair" "pipeline_key" {
  key_name   = "data-eng-pipeline"
  public_key = file("/home/rocio/.ssh/data-eng-pipeline.pub")
}
